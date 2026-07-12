package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
@Schema(description = "医生咨询消息请求")
public class DoctorConsultMessageDTO {
    @Schema(description = "文本内容")
    private String content;

    @Schema(description = "附件URL")
    private String attachmentUrl;

    @Schema(description = "附件名称")
    private String attachmentName;

    @AssertTrue(message = "消息内容或附件不能为空")
    public boolean isMessageValid() {
        return StringUtils.hasText(content) || StringUtils.hasText(attachmentUrl);
    }
}
