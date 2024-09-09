package org.example.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class Product {
    private static RedisTemplate<String, Object> redisTemplate;

    private String id;  // 改为String类型
    private double price;

    // 静态方法来设置RedisTemplate
    public static void setRedisTemplate(RedisTemplate<String, Object> template) {
        redisTemplate = template;
    }

    @Autowired
    private ObjectMapper objectMapper;

    public List<PriceChange> getPriceHistory() {
        if (redisTemplate == null) {
            return new ArrayList<>();
        }
        String key = "product:" + this.id + ":price_history";
        String history = (String) redisTemplate.opsForValue().get(key);
        if (history != null) {
            try {
                return objectMapper.readValue(history, new TypeReference<List<PriceChange>>() {});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }

    // 内部类用于表示格变化
    public static class PriceChange {
        private double oldPrice;
        private double newPrice;
        private Instant timestamp;

        public PriceChange() {} // 默认构造函数

        public PriceChange(double oldPrice, double newPrice, Instant timestamp) {
            this.oldPrice = oldPrice;
            this.newPrice = newPrice;
            this.timestamp = timestamp;
        }

        public double getOldPrice() {
            return oldPrice;
        }

        public void setOldPrice(double oldPrice) {
            this.oldPrice = oldPrice;
        }

        public double getNewPrice() {
            return newPrice;
        }

        public void setNewPrice(double newPrice) {
            this.newPrice = newPrice;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void updatePrice(double newPrice) {
        double oldPrice = this.price;
        this.price = newPrice;

        PriceChange change = new PriceChange(oldPrice, newPrice, Instant.now());
        List<PriceChange> history = getPriceHistory();
        history.add(change);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonHistory = mapper.writeValueAsString(history);
            String key = "product:" + this.id + ":price_history";
            redisTemplate.opsForValue().set(key, jsonHistory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ... 其他getter和setter方法 ...
}