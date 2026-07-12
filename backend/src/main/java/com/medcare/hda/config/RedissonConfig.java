package com.medcare.hda.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** Redisson 客户端（分布式锁）。手动装配，复用 spring.data.redis.* 配置。 */
@Slf4j
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;
    @Value("${spring.data.redis.port:6379}")
    private int port;
    @Value("${spring.data.redis.database:0}")
    private int database;
    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setPassword(StringUtils.hasText(password) ? password : null)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(16);
        log.info("Redisson 初始化: redis://{}:{} db={}", host, port, database);
        return Redisson.create(config);
    }
}
