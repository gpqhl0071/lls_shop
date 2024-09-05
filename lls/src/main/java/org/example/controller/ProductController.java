package org.example.controller;

import org.example.config.ApiConfig;
import org.example.service.ProductService;
import org.example.model.ProductSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProductSummary> getProducts() {
        return productService.getProductSummaries();
    }

    @GetMapping(path = "/html", produces = MediaType.TEXT_HTML_VALUE)
    public String getProductsHtml(@RequestParam(defaultValue = "0") int status) {
        List<ProductSummary> products = productService.getProductSummariesByStatus(status);
        return generateHtmlReport(products, status);
    }

    private String generateHtmlReport(List<ProductSummary> products, int status) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html lang='zh-CN'>")
            .append("<head>")
            .append("<meta charset='UTF-8'>")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            .append("<title>商品列表</title>")
            .append("<style>")
            .append("  .table-container { max-height: 500px; overflow-y: auto; }")
            .append("  table { border-collapse: collapse; width: 100%; }")
            .append("  th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
            .append("  thead { position: sticky; top: 0; background-color: #f2f2f2; }")
            .append("  tr:hover { background-color: #e6f2ff; }") // 添加悬停效果
            .append("  tr:nth-child(even) { background-color: #f9f9f9; }") // 添加斑马纹效果
            .append("  tr:nth-child(even):hover { background-color: #e6f2ff; }") // 确保悬停效果在偶数行也生效
            .append("</style>")
            .append("<div class='status-filter'>")
            .append("<label for='status'>账号状态：</label>")
            .append("<select id='status' onchange='changeStatus(this.value)'>")
            .append("<option value='").append(ApiConfig.STATUS_ALL).append("'")
            .append(status == ApiConfig.STATUS_ALL ? " selected" : "").append(">全部</option>")
            .append("<option value='").append(ApiConfig.STATUS_UNSOLD).append("'")
            .append(status == ApiConfig.STATUS_UNSOLD ? " selected" : "").append(">未开售</option>")
            .append("<option value='").append(ApiConfig.STATUS_SOLD).append("'")
            .append(status == ApiConfig.STATUS_SOLD ? " selected" : "").append(">已开卖</option>")
            .append("</select>")
            .append("</div>")
            .append("<body>")
            .append("<h1>商品列表</h1>")
            .append("<p>总商品数: ").append(products.size()).append("</p>")
            .append("<input type='text' id='searchInput' placeholder='搜索商品...' onkeyup='searchTable()'>")
            .append("<div class='table-container'>")
            .append("<table id='productTable'>")
            .append("<thead>")
            .append("<tr>")
            .append("<th>Flag ID</th>")
            .append("<th>玩家名称</th>") // 新增列
            .append("<th>价格</th>")
            .append("<th class='sort-btn' onclick='sortTable(2, true)'>开售时间 ▲▼</th>")
            .append("<th>倒计时</th>")
            .append("<th>收藏数</th>")
            .append("<th>服务器ID</th>")
            .append("<th>战力</th>")
            .append("<th>VIP等级</th>")
            .append("<th>击杀积分</th>")
            .append("<th>操作</th>")
            .append("</tr>")
            .append("</thead>")
            .append("<tbody>");

        LocalDateTime now = LocalDateTime.now();
        products.forEach(product -> {
            LocalDateTime publishTime = LocalDateTime.parse(product.getPublishTime(), formatter);
            LocalDateTime saleTime = publishTime.plusDays(2);
            Duration duration = Duration.between(now, saleTime);
            String countdownClass = duration.isNegative() ? "" : " class='countdown'";
            String dataEndTime = saleTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            html.append("<tr>")
                .append("<td>").append(product.getFlagId()).append("</td>")
                .append("<td>").append(product.getName()).append("</td>") // 新增列
                .append("<td>").append(String.format("%.2f", product.getAmount())).append("元</td>")
                .append("<td data-sort='").append(saleTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("'>")
                .append(saleTime.format(formatter)).append("</td>")
                .append("<td").append(countdownClass).append(" data-end-time='").append(dataEndTime).append("'>")
                .append(duration.isNegative() ? "已开售" : "计算中...").append("</td>")
                .append("<td>").append(product.getFavoriteCount()).append("</td>")
                .append("<td>").append(product.getServerId()).append("</td>")
                .append("<td>").append(product.getCombatPower()).append("</td>")
                .append("<td>").append(product.getVipLevel()).append("</td>")
                .append("<td>").append(product.getKillScore()).append("</td>")
                .append("<td><a href='https://trade.lilith.com/detail/").append(product.getFlagId()).append("' target='_blank'>查看详情</a></td>")
                .append("</tr>");
        });

        html.append("</tbody>")
            .append("</table>")
            .append("</div>")
            .append("<script src='/js/table-functions.js'></script>")
            .append("<script>")
            .append("function changeStatus(status) {")
            .append("  window.location.href = '/api/products/html?status=' + status;")
            .append("}")
            .append("</script>")
            .append("</body></html>");

        return html.toString();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e) {
        return generateErrorPage(e);
    }

    private String generateErrorPage(Exception e) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html lang='zh-CN'>")
            .append("<head>")
            .append("<meta charset='UTF-8'>")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            .append("<title>错误页面</title>")
            .append("<style>")
            .append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 800px; margin: 0 auto; padding: 20px; }")
            .append("h1 { color: #d9534f; }")
            .append("pre { background-color: #f8f9fa; padding: 15px; border-radius: 5px; overflow-x: auto; }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<h1>发生错误</h1>")
            .append("<p>很抱歉,在处理您的请求时发生了错误。</p>")
            .append("<h2>错误详情:</h2>")
            .append("<pre>")
            .append(e.toString())
            .append("\n\n")
            .append(getStackTraceAsString(e))
            .append("</pre>")
            .append("</body></html>");

        return html.toString();
    }

    private String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}