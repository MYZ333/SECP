package com.medcare.hda.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码实现。
 * 配置 hda.sms.access-key-id / access-key-secret / sign-name / template-code 后走阿里云真实短信；
 * 任一项为空则为 mock 模式：验证码打印到后端日志（方便开发与演示）。
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    private final StringRedisTemplate redis;

    @Value("${hda.sms.access-key-id:}")
    private String accessKeyId;
    @Value("${hda.sms.access-key-secret:}")
    private String accessKeySecret;
    @Value("${hda.sms.sign-name:}")
    private String signName;
    @Value("${hda.sms.template-code:}")
    private String templateCode;
    @Value("${hda.sms.endpoint:dysmsapi.aliyuncs.com}")
    private String endpoint;

    /** Redis key 前缀 */
    private static final String CODE_KEY = "hda:sms:code:";   // 验证码本体, TTL 5min
    private static final String SENT_KEY = "hda:sms:sent:";   // 60 秒重发间隔标记
    private static final String DAY_KEY  = "hda:sms:day:";    // 同手机号每日计数
    private static final String IP_KEY   = "hda:sms:ip:";     // 同 IP 每小时计数
    private static final String FAIL_KEY = "hda:sms:fail:";   // 连续错误计数

    private static final int CODE_TTL_MINUTES = 5;
    private static final int RESEND_INTERVAL_SECONDS = 60;
    private static final int MAX_PER_PHONE_PER_DAY = 10;
    private static final int MAX_PER_IP_PER_HOUR = 20;
    private static final int MAX_FAIL_TIMES = 5;

    private volatile Client client;

    public SmsServiceImpl(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean sendCode(String phone, String ip) {
        // —— 限流三道闸 ——
        if (Boolean.TRUE.equals(redis.hasKey(SENT_KEY + phone))) {
            throw new BusinessException(ResultCode.SMS_TOO_FREQUENT);
        }
        String day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long phoneCount = redis.opsForValue().increment(DAY_KEY + phone + ":" + day);
        if (phoneCount != null && phoneCount == 1) {
            redis.expire(DAY_KEY + phone + ":" + day, 24, TimeUnit.HOURS);
        }
        if (phoneCount != null && phoneCount > MAX_PER_PHONE_PER_DAY) {
            throw new BusinessException(ResultCode.SMS_LIMIT_EXCEEDED);
        }
        String hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        Long ipCount = redis.opsForValue().increment(IP_KEY + ip + ":" + hour);
        if (ipCount != null && ipCount == 1) {
            redis.expire(IP_KEY + ip + ":" + hour, 1, TimeUnit.HOURS);
        }
        if (ipCount != null && ipCount > MAX_PER_IP_PER_HOUR) {
            throw new BusinessException(ResultCode.SMS_LIMIT_EXCEEDED);
        }

        // —— 生成并存储验证码 ——
        String code = RandomUtil.randomNumbers(6);
        redis.opsForValue().set(CODE_KEY + phone, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redis.delete(FAIL_KEY + phone);

        boolean mock = StrUtil.hasBlank(accessKeyId, accessKeySecret, signName, templateCode);
        if (mock) {
            log.info("【短信MOCK】手机号 {} 的验证码: {}（{}分钟内有效。配置 hda.sms.* 后将改为阿里云真实短信）",
                    phone, code, CODE_TTL_MINUTES);
        } else {
            sendByAliyun(phone, code);
        }
        // 发送成功后再打 60 秒重发间隔标记
        redis.opsForValue().set(SENT_KEY + phone, "1", RESEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
        return mock;
    }

    @Override
    public void verifyCode(String phone, String code) {
        String saved = redis.opsForValue().get(CODE_KEY + phone);
        if (saved == null) {
            throw new BusinessException(ResultCode.SMS_CODE_EXPIRED);
        }
        if (!saved.equals(code)) {
            Long fails = redis.opsForValue().increment(FAIL_KEY + phone);
            if (fails != null && fails == 1) {
                redis.expire(FAIL_KEY + phone, CODE_TTL_MINUTES, TimeUnit.MINUTES);
            }
            if (fails != null && fails >= MAX_FAIL_TIMES) {
                redis.delete(CODE_KEY + phone);
                redis.delete(FAIL_KEY + phone);
                throw new BusinessException(ResultCode.SMS_CODE_INVALIDATED);
            }
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }
        // 一次性：校验通过即失效
        redis.delete(CODE_KEY + phone);
        redis.delete(FAIL_KEY + phone);
    }

    /** 调用阿里云短信 API（模板须含 ${code} 变量） */
    private void sendByAliyun(String phone, String code) {
        try {
            SendSmsRequest req = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");
            SendSmsResponse resp = getClient().sendSms(req);
            String respCode = resp.getBody().getCode();
            if (!"OK".equals(respCode)) {
                log.error("阿里云短信发送失败: code={}, message={}", respCode, resp.getBody().getMessage());
                throw new BusinessException(ResultCode.SMS_SEND_FAIL);
            }
            log.info("阿里云短信已发送: phone={}, bizId={}", phone, resp.getBody().getBizId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("阿里云短信调用异常", e);
            throw new BusinessException(ResultCode.SMS_SEND_FAIL);
        }
    }

    private Client getClient() throws Exception {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    Config config = new Config()
                            .setAccessKeyId(accessKeyId)
                            .setAccessKeySecret(accessKeySecret);
                    config.endpoint = endpoint;
                    client = new Client(config);
                }
            }
        }
        return client;
    }
}
