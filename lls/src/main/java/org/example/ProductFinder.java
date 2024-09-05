package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.ApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ProductFinder {

    private static final Logger logger = LoggerFactory.getLogger(ProductFinder.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EmailService emailService;

    static class Product {
        String flagId;
        String title;
        double price;
        String listingTime;
        String saleTime;
        String countdown;

        Product(String flagId, String title, double price, String listingTime, String saleTime, String countdown) {
            this.flagId = flagId;
            this.title = title;
            this.price = price;
            this.listingTime = listingTime;
            this.saleTime = saleTime;
            this.countdown = countdown;
        }
    }

    public List<Product> findUpcomingCheapProducts(int hoursThreshold, double priceThreshold) {
        List<Product> allProducts = new ArrayList<>();
        int page = 1;
        List<Product> pageProducts;
        do {
            pageProducts = fetchAndParseData(page, hoursThreshold, priceThreshold);
            allProducts.addAll(pageProducts);
            page++;
            if (pageProducts.size() < 100) {  // 如果获取的数据少于100条,说明已经到达最后一页
                break;
            }
        } while (page <= 5);  // 最多获取5页数据
        return allProducts;
    }

    private List<Product> fetchAndParseData(int page, int hoursThreshold, double priceThreshold) {
        String jsonData = fetchDataFromApi(page);
        return parseJsonData(jsonData, hoursThreshold, priceThreshold);
    }

    private String fetchDataFromApi(int page) {
        try {
            logger.info("正在获取第 {} 页数据", page);
            return restTemplate.getForObject(ApiConfig.getApiUrl(ApiConfig.STATUS_ALL, page, 100), String.class);
        } catch (Exception e) {
            logger.error("从API获取数据时发生错误 (页码: {})", page, e);
            throw new RuntimeException("无法从API获取数据 (页码: " + page + ")", e);
        }
    }

    private List<Product> parseJsonData(String jsonData, int hoursThreshold, double priceThreshold) {
        List<Product> upcomingCheapProducts = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jsonData);
            JsonNode productList = root.path("data").path("product_list");

            if (productList.isMissingNode() || !productList.isArray()) {
                logger.error("API返回的数据格式不正确");
                throw new RuntimeException("API返回的数据格式不正确");
            }

            ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (JsonNode product : productList) {
                try {
                    ZonedDateTime listingTimeBeijing = parseDateTime(product.path("public_start_at").asText());
                    ZonedDateTime saleTimeBeijing = listingTimeBeijing.plusDays(2);

                    long seconds = ChronoUnit.SECONDS.between(currentTime, saleTimeBeijing);
                    String countdown = String.format("%02d:%02d:%02d",
                            seconds / 3600, (seconds % 3600) / 60, seconds % 60);

                    double priceInYuan = product.path("price").asDouble() / 100;

                    boolean meetsTimeCondition = seconds >= 0 && seconds <= hoursThreshold * 3600;
                    boolean meetsPriceCondition = priceInYuan < priceThreshold;

                    if (meetsTimeCondition && meetsPriceCondition) {
                        upcomingCheapProducts.add(new Product(
                                product.path("flag_id").asText(),
                                product.path("title").asText(),
                                priceInYuan,
                                listingTimeBeijing.format(outputFormatter),
                                saleTimeBeijing.format(outputFormatter),
                                countdown
                        ));
                    }
                } catch (Exception e) {
                    logger.warn("处理商品时发生错误: {}", product.path("flag_id").asText(), e);
                }
            }
        } catch (Exception e) {
            logger.error("解析JSON数据时发生错误", e);
            throw new RuntimeException("解析产品数据失败", e);
        }

        return upcomingCheapProducts;
    }

    private ZonedDateTime parseDateTime(String dateString) {
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC")),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC")),
                DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                Instant instant = Instant.from(formatter.parse(dateString));
                return ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }
        logger.error("无法解析日期: {}", dateString);
        throw new IllegalArgumentException("无法解析日期: " + dateString);
    }

    public void handle() {
        logger.info("开始测试 ProductFinder...");

        int hoursThreshold = 1; // 1小时内开售
        double priceThreshold = 500; // 价格低于500元

        try {
            List<ProductFinder.Product> products = findUpcomingCheapProducts(hoursThreshold, priceThreshold);
            logger.info("总共找到 {} 个符合条件的商品", products.size());
            String htmlReport = generateHtmlReport(products);
            logger.info("生成的HTML报告: {}", htmlReport);

            if (products.size() > 0) {
                emailService.sendEmail("gpqhl0071@126.com", "即将开售", htmlReport);
                logger.info("邮件已发送");
            } else {
                logger.info("没有找到符合条件的商品");
            }
        } catch (Exception e) {
            logger.error("处理ProductFinder时发生错误", e);
        }
    }

    public String generateHtmlReport(List<ProductFinder.Product> products) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>商品查询结果</h2>");
        html.append("<p>找到 ").append(products.size()).append(" 个符合条件的商品：</p>");

        for (ProductFinder.Product product : products) {
            html.append("<div style='border: 1px solid #ddd; padding: 10px; margin-bottom: 10px;'>");
            html.append("<p><strong>商品ID:</strong> ").append(product.flagId).append("</p>");
            html.append("<p><strong>商品URL:</strong> <a href='https://trade.lilith.com/detail/").append(product.flagId).append("'>").append("https://trade.lilith.com/detail/").append(product.flagId).append("</a></p>");
            html.append("<p><strong>标题:</strong> ").append(product.title).append("</p>");
            html.append("<p><strong>价格:</strong> ").append(product.price).append(" 元</p>");
            html.append("<p><strong>开售时间:</strong> ").append(product.saleTime).append("</p>");
            html.append("<p><strong>倒计时:</strong> ").append(product.countdown).append("</p>");
            html.append("</div>");
        }

        html.append("<p>查询完成。</p>");
        html.append("</body></html>");
        return html.toString();
    }
}