package com.medcare.hda;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智慧医养大数据公共服务平台 - 个人健康档案系统 后端启动类
 */
@SpringBootApplication
@MapperScan("com.medcare.hda.mapper")
public class HdaApplication {
    public static void main(String[] args) {
        SpringApplication.run(HdaApplication.class, args);
    }
}
