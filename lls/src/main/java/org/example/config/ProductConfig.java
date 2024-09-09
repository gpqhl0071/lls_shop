package org.example.config;

import org.example.model.Product;
import org.example.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;

@Configuration
@EnableScheduling
public class ProductConfig {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductService productService;

    @PostConstruct
    public void init() {
        Product.setRedisTemplate(redisTemplate);
    }

    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void updatePriceData() {
        productService.initializeOrUpdatePriceData();
    }
}