package org.example.controller;

import org.example.model.Product;
import org.example.model.ProductSummary;
import org.example.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 处理产品页面的Web控制器
 * 使用Thymeleaf模板渲染HTML页面
 */
@Controller
public class WebProductController {

    private static final Logger logger = LoggerFactory.getLogger(WebProductController.class);
    private static final int PAGE_SIZE_DEFAULT = 20;
    private static final int MAX_PAGE_BUTTONS = 5;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ProductService productService;

    /**
     * 显示产品列表页面
     */
    @GetMapping("/products")
    public String productList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "2") int status,
            @RequestParam(required = false) String serverId,
            @RequestParam(required = false, defaultValue = "saleTime") String sort,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            Model model) {
        
        logger.info("获取产品列表页面: page={}, size={}, status={}, serverId={}, sort={}, direction={}", 
            page, size, status, serverId, sort, direction);
        
        try {
            // 获取产品列表
            List<ProductSummary> allProducts;
            if (status == -1) {
                allProducts = productService.getProductSummaries();
            } else {
                if (serverId != null && !serverId.isEmpty()) {
                    try {
                        int serverIdInt = Integer.parseInt(serverId);
                        allProducts = productService.getProductSummariesByStatusAndServerId(status, serverIdInt);
                    } catch (NumberFormatException e) {
                        allProducts = productService.getProductSummariesByStatus(status);
                    }
                } else {
                    allProducts = productService.getProductSummariesByStatus(status);
                }
            }
            
            // 解析开售时间
            Map<String, LocalDateTime> saleTimes = new HashMap<>();
            for (ProductSummary product : allProducts) {
                try {
                    String publishTimeStr = product.getPublishTime();
                    LocalDateTime publishTime = LocalDateTime.parse(publishTimeStr, formatter);
                    // 开售时间为发布时间加2天
                    LocalDateTime saleTime = publishTime.plusDays(2);
                    saleTimes.put(product.getFlagId(), saleTime);
                } catch (Exception e) {
                    logger.error("解析开售时间失败: " + product.getFlagId(), e);
                    saleTimes.put(product.getFlagId(), LocalDateTime.now());
                }
            }
            
            // 如果按开售时间排序
            if ("saleTime".equals(sort)) {
                // 按开售时间排序
                allProducts.sort((p1, p2) -> {
                    LocalDateTime time1 = saleTimes.getOrDefault(p1.getFlagId(), LocalDateTime.now());
                    LocalDateTime time2 = saleTimes.getOrDefault(p2.getFlagId(), LocalDateTime.now());
                    return "asc".equalsIgnoreCase(direction) ? 
                           time1.compareTo(time2) : time2.compareTo(time1);
                });
                
                // 记录排序已应用
                model.addAttribute("sortApplied", true);
                model.addAttribute("sortField", "saleTime");
                model.addAttribute("sortDirection", direction);
            }
            // 如果按价格排序
            else if ("price".equals(sort)) {
                // 按价格排序
                allProducts.sort((p1, p2) -> {
                    BigDecimal price1 = p1.getAmount();
                    BigDecimal price2 = p2.getAmount();
                    return "asc".equalsIgnoreCase(direction) ? 
                           price1.compareTo(price2) : price2.compareTo(price1);
                });
                
                // 记录排序已应用
                model.addAttribute("sortApplied", true);
                model.addAttribute("sortField", "price");
                model.addAttribute("sortDirection", direction);
            }
            
            // 计算分页信息
            int totalElements = allProducts.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            // 确保页码在有效范围内
            if (page < 0) page = 0;
            if (page >= totalPages && totalPages > 0) page = totalPages - 1;
            
            // 获取当前页的数据
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalElements);
            List<ProductSummary> products = (fromIndex < totalElements) 
                    ? allProducts.subList(fromIndex, toIndex) 
                    : new ArrayList<>();
            
            // 计算统计信息
            BigDecimal averagePrice = BigDecimal.ZERO;
            BigDecimal maxPrice = products.isEmpty() ? null : products.get(0).getAmount();
            BigDecimal minPrice = products.isEmpty() ? null : products.get(0).getAmount();
            
            if (!products.isEmpty()) {
                BigDecimal total = BigDecimal.ZERO;
                for (ProductSummary product : products) {
                    total = total.add(product.getAmount());
                    if (maxPrice.compareTo(product.getAmount()) < 0) {
                        maxPrice = product.getAmount();
                    }
                    if (minPrice.compareTo(product.getAmount()) > 0) {
                        minPrice = product.getAmount();
                    }
                }
                averagePrice = total.divide(BigDecimal.valueOf(products.size()), 2, BigDecimal.ROUND_HALF_UP);
            }
            
            // 获取所有服务器ID列表用于过滤
            Set<Integer> servers = products.stream()
                    .map(ProductSummary::getServerId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            // 计算分页导航的起始和结束页
            int pageStart = Math.max(0, page - (MAX_PAGE_BUTTONS / 2));
            int pageEnd = Math.min(totalPages - 1, pageStart + MAX_PAGE_BUTTONS - 1);
            if (pageEnd - pageStart + 1 < MAX_PAGE_BUTTONS && pageStart > 0) {
                pageStart = Math.max(0, pageEnd - MAX_PAGE_BUTTONS + 1);
            }
            
            // 添加模型属性
            model.addAttribute("products", products);
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("status", status);
            model.addAttribute("serverId", serverId);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", totalElements);
            model.addAttribute("pageStart", pageStart);
            model.addAttribute("pageEnd", pageEnd);
            model.addAttribute("averagePrice", averagePrice);
            model.addAttribute("maxPrice", maxPrice);
            model.addAttribute("minPrice", minPrice);
            model.addAttribute("servers", servers);
            model.addAttribute("saleTimes", saleTimes);
            
            // 筛选精品号：价格低于500元且收藏数大于10
            List<ProductSummary> premiumProducts = allProducts.stream()
                .filter(p -> p.getAmount().doubleValue() < 500.0 && p.getFavoriteCount() > 10)
                .limit(10) // 限制最多展示10个
                .collect(Collectors.toList());
            model.addAttribute("premiumProducts", premiumProducts);
            
            return "product/list";
        } catch (Exception e) {
            logger.error("获取产品列表出错", e);
            model.addAttribute("error", e.getMessage());
            return "error/general";
        }
    }
    
    /**
     * 显示产品详情页面
     */
    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable String id, Model model) {
        logger.info("获取产品详情页面: id={}", id);
        
        try {
            // 创建产品对象并设置ID
            Product product = new Product();
            product.setId(id);
            
            // 获取产品列表
            List<ProductSummary> allProducts = productService.getProductSummaries();
            
            // 查找匹配的产品
            ProductSummary productSummary = allProducts.stream()
                .filter(p -> p.getFlagId().equals(id))
                .findFirst()
                .orElse(null);
            
            if (productSummary == null) {
                return "error/404";
            }
            
            // 构建完整的产品模型
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", id);
            productMap.put("name", productSummary.getName());
            productMap.put("price", productSummary.getAmount());
            productMap.put("originalPrice", productSummary.getAmount()); // 暂用当前价格作为原价
            productMap.put("imageUrl", productSummary.getAvatarUrl());
            productMap.put("description", productSummary.getIntroduction());
            productMap.put("category", "游戏账号");
            productMap.put("brand", "王国纪元");
            productMap.put("stock", 1);
            
            // 获取价格历史
            List<Product.PriceChange> priceHistory = productService.getPriceHistory(id);
            productMap.put("priceHistory", priceHistory);
            
            // 如果有价格历史，根据第一个记录设置原价
            if (priceHistory != null && !priceHistory.isEmpty()) {
                double originalPrice = priceHistory.get(0).getOldPrice();
                productMap.put("originalPrice", BigDecimal.valueOf(originalPrice));
                
                // 计算折扣率
                if (originalPrice > 0) {
                    double currentPrice = productSummary.getAmount().doubleValue();
                    double discount = 100 - (currentPrice / originalPrice * 100);
                    if (discount > 0) {
                        productMap.put("discount", Math.round(discount));
                    }
                }
            }
            
            model.addAttribute("product", productMap);
            
            // 获取相关推荐
            BigDecimal productPrice = productSummary.getAmount();
            BigDecimal lowerBound = productPrice.multiply(new BigDecimal("0.8"));
            BigDecimal upperBound = productPrice.multiply(new BigDecimal("1.2"));
            
            List<ProductSummary> relatedProducts = allProducts.stream()
                .filter(p -> !p.getFlagId().equals(id)) // 排除当前产品
                .filter(p -> p.getAmount().compareTo(lowerBound) >= 0 && p.getAmount().compareTo(upperBound) <= 0) // 价格范围
                .limit(5) // 限制最多5个推荐
                .collect(Collectors.toList());
            
            model.addAttribute("relatedProducts", relatedProducts);
            
            return "product/detail";
        } catch (Exception e) {
            logger.error("获取产品详情出错", e);
            model.addAttribute("error", e.getMessage());
            return "error/general";
        }
    }
    
    /**
     * 通用错误处理
     */
    @GetMapping("/error")
    public String handleError(Model model) {
        return "error/general";
    }
} 