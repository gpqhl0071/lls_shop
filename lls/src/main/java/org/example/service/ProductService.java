package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.ProductSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.example.config.ApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private static final int PAGES_TO_FETCH = 5;
    private static final int PAGE_SIZE = 100;

    @Autowired
    private RestTemplate restTemplate;

    public List<ProductSummary> getProductSummariesByStatus(int status) {
        List<ProductSummary> allProducts = new ArrayList<>();
        try {
            for (int page = 1; page <= PAGES_TO_FETCH; page++) {
                String jsonData = restTemplate.getForObject(
                    ApiConfig.getApiUrl(status, page, PAGE_SIZE), 
                    String.class
                );
                List<ProductSummary> pageProducts = parseJsonData(jsonData);
                allProducts.addAll(pageProducts);
                
                logger.info("获取第 {} 页数据, 共 {} 条记录", page, pageProducts.size());
                
                if (pageProducts.size() < PAGE_SIZE) {
                    // 如果获取的数据少于页面大小,说明已经到达最后一页
                    break;
                }
            }
            logger.info("总共获取 {} 条记录", allProducts.size());
            return allProducts;
        } catch (Exception e) {
            logger.error("获取产品摘要时发生错误", e);
            throw new RuntimeException("获取产品数据失败", e);
        }
    }

    public List<ProductSummary> getProductSummaries() {
        return getProductSummariesByStatus(ApiConfig.STATUS_ALL);
    }

    private List<ProductSummary> parseJsonData(String jsonData) {
        List<ProductSummary> productSummaries = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jsonData);
            JsonNode productList = root.path("data").path("product_list");

            if (productList.isMissingNode() || !productList.isArray()) {
                throw new RuntimeException("API返回的数据格式不正确");
            }

            for (JsonNode product : productList) {
                String flagId = product.path("flag_id").asText();
                BigDecimal amount = BigDecimal.valueOf(product.path("price").asDouble() / 100);
                String publishTime = formatTime(product.path("public_start_at").asText());
                String introduction = product.path("introduction").asText();
                int favoriteCount = product.path("favorite_count").asInt();
                int serverId = product.path("server_id").asInt();

                long combatPower = 0;
                int vipLevel = 0;
                long killScore = 0;
                String name = "";

                try {
                    JsonNode introNode = mapper.readTree(introduction);
                    combatPower = introNode.path("detail").path("combat_power").asLong(0);
                    vipLevel = introNode.path("detail").path("vip").asInt(0);
                    name = introNode.path("name").asText("");

                    JsonNode shortDescription = introNode.path("short_description");
                    if (shortDescription.isArray()) {
                        for (JsonNode item : shortDescription) {
                            if ("击杀积分".equals(item.path("key").asText())) {
                                killScore = item.path("value").asLong(0);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("解析introduction字段失败: {}", introduction, e);
                }

                productSummaries.add(new ProductSummary(flagId, amount, publishTime, introduction, favoriteCount,
                        serverId, combatPower, vipLevel, killScore, name));
            }
        } catch (Exception e) {
            logger.error("解析JSON数据时发生错误", e);
            throw new RuntimeException("解析产品数据失败", e);
        }

        return productSummaries;
    }

    private String formatTime(String timeString) {
        Instant instant = Instant.parse(timeString);
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Shanghai"))
                .format(instant);
    }
}