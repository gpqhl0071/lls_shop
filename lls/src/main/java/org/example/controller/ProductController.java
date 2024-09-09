package org.example.controller;

import org.example.config.ApiConfig;
import org.example.service.ProductService;
import org.example.model.ProductSummary;
import org.example.model.Product;
import org.example.form.ProductForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private Product product;

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
            .append("  body { font-family: Arial, sans-serif; }")
            .append("  .table-container { max-height: 500px; overflow-y: auto; }")
            .append("  table { border-collapse: collapse; width: 100%; }")
            .append("  th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
            .append("  thead { position: sticky; top: 0; background-color: #f2f2f2; }")
            .append("  tr:hover { background-color: #e6f2ff; }")
            .append("  tr:nth-child(even) { background-color: #f9f9f9; }")
            .append("  tr:nth-child(even):hover { background-color: #e6f2ff; }")
            .append("  .filter-container { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }")
            .append("  .filter-container > div { display: flex; align-items: center; }")
            .append("  .filter-container label { margin-right: 10px; }")
            .append("  .filter-container select, .filter-container input { padding: 5px; }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<h1>商品列表</h1>")
            .append("<p>总商品数: ").append(products.size()).append("</p>")
            .append("<div class='filter-container'>")
            .append("  <div>")
            .append("    <label for='status'>账号状态：</label>")
            .append("    <select id='status' onchange='changeStatus(this.value)'>")
            .append("      <option value='").append(ApiConfig.STATUS_ALL).append("'")
            .append(status == ApiConfig.STATUS_ALL ? " selected" : "").append(">全部</option>")
            .append("      <option value='").append(ApiConfig.STATUS_UNSOLD).append("'")
            .append(status == ApiConfig.STATUS_UNSOLD ? " selected" : "").append(">未开售</option>")
            .append("      <option value='").append(ApiConfig.STATUS_SOLD).append("'")
            .append(status == ApiConfig.STATUS_SOLD ? " selected" : "").append(">已开卖</option>")
            .append("    </select>")
            .append("  </div>")
            .append("  <div>")
            .append("    <label for='serverId'>服务器ID：</label>")
            .append("    <input type='number' id='serverId' onchange='filterTable()' placeholder='输入服务器ID'>")
            .append("  </div>")
            .append("  <div>")
            .append("    <label for='searchInput'>搜索商品：</label>")
            .append("    <input type='text' id='searchInput' onkeyup='filterTable()' placeholder='输入关键字...'>")
            .append("  </div>")
            .append("</div>")
            .append("<div class='table-container'>")
            .append("<table id='productTable'>")
            .append("<thead>")
            .append("<tr>")
            .append("<th>头像</th>")
            .append("<th>服务器ID</th>")
            .append("<th>战力</th>")
            .append("<th>VIP等级</th>")
            .append("<th>玩家名称</th>")
            .append("<th>当前价格</th>")
            .append("<th>原价</th>")
            .append("<th class='sort-btn' onclick='sortTable(5)'>开售时间 ▲▼</th>")
            .append("<th>倒计时</th>")
            .append("<th>收藏数</th>")
            .append("<th>击杀积分</th>")
            .append("<th>操作</th>")
            .append("</tr>")
            .append("</thead>")
            .append("<tbody>");

        LocalDateTime now = LocalDateTime.now();
        products.forEach(product -> {
            try {
                LocalDateTime publishTime = LocalDateTime.parse(product.getPublishTime(), formatter);
                LocalDateTime saleTime = publishTime.plusDays(2);
                String saleTimeStr = saleTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                
                String favoriteCountStyle = product.getFavoriteCount() > 10 ? " style='color: red;'" : "";
                
                // 获取价格历史
                List<Product.PriceChange> priceHistory = productService.getPriceHistory(product.getFlagId());
                double originalPrice = priceHistory.isEmpty() ? product.getAmount().doubleValue() : priceHistory.get(0).getOldPrice();
                
                // 比较当前价格和原价
                String currentPriceStyle = (Math.abs(product.getAmount().doubleValue() - originalPrice) > 0.01) ? "color: red; font-weight: bold;" : "";
                
                html.append("<tr>")
                    .append("<td><img src='").append(product.getAvatarUrl()).append("' alt='头像' style='width: 30px; height: 30px; border-radius: 50%;'></td>")
                    .append("<td>").append(product.getServerId()).append("</td>")
                    .append("<td>").append(product.getCombatPower()).append("</td>")
                    .append("<td>").append(product.getVipLevel()).append("</td>")
                    .append("<td>").append(product.getName()).append("</td>")
                    .append("<td><span style='").append(currentPriceStyle).append("'>").append(String.format("%.2f", product.getAmount())).append("元</span></td>")
                    .append("<td>").append(String.format("%.2f", originalPrice)).append("元</td>")
                    .append("<td data-sort='").append(saleTimeStr).append("'>")
                    .append(saleTime.format(formatter)).append("</td>")
                    .append("<td class='countdown' data-saletime='").append(saleTimeStr).append("'></td>")
                    .append("<td").append(favoriteCountStyle).append(">").append(product.getFavoriteCount()).append("</td>")
                    .append("<td>").append(product.getKillScore()).append("</td>")
                    .append("<td><a href='https://trade.lilith.com/detail/").append(product.getFlagId()).append("' target='_blank'>查看详情</a></td>")
                    .append("</tr>");
            } catch (Exception e) {
                logger.error("处理产品时发生错误: " + product.getFlagId(), e);
                // 可以选择跳过这个产品或者添加一个错误信息到HTML中
            }
        });

        html.append("</tbody>")
            .append("</table>")
            .append("</div>")
            .append("<script>")
            .append("function changeStatus(status) {")
            .append("  window.location.href = '/api/products/html?status=' + status;")
            .append("}")
            .append("function filterTable() {")
            .append("  var input, serverIdInput, filter, serverIdFilter, table, tr, td, i, txtValue;")
            .append("  input = document.getElementById('searchInput');")
            .append("  serverIdInput = document.getElementById('serverId');")
            .append("  filter = input.value.toUpperCase();")
            .append("  serverIdFilter = serverIdInput.value;")
            .append("  table = document.getElementById('productTable');")
            .append("  tr = table.getElementsByTagName('tr');")
            .append("  for (i = 0; i < tr.length; i++) {")
            .append("    var display = '';")
            .append("    var serverIdMatch = true;")
            .append("    td = tr[i].getElementsByTagName('td');")
            .append("    if (td.length > 0) {")
            .append("      if (serverIdFilter && td[1].textContent != serverIdFilter) {")
            .append("        serverIdMatch = false;")
            .append("      }")
            .append("      if (serverIdMatch) {")
            .append("        for (var j = 0; j < td.length; j++) {")
            .append("          if (td[j]) {")
            .append("            txtValue = td[j].textContent || td[j].innerText;")
            .append("            if (txtValue.toUpperCase().indexOf(filter) > -1) {")
            .append("              display = '';")
            .append("              break;")
            .append("            } else {")
            .append("              display = 'none';")
            .append("            }")
            .append("          }")
            .append("        }")
            .append("      } else {")
            .append("        display = 'none';")
            .append("      }")
            .append("    }")
            .append("    tr[i].style.display = display;")
            .append("  }")
            .append("}")
            .append("function sortTable(n) {")
            .append("  var table, rows, switching, i, x, y, shouldSwitch, dir, switchcount = 0;")
            .append("  table = document.getElementById('productTable');")
            .append("  switching = true;")
            .append("  dir = 'asc';")
            .append("  while (switching) {")
            .append("    switching = false;")
            .append("    rows = table.rows;")
            .append("    for (i = 1; i < (rows.length - 1); i++) {")
            .append("      shouldSwitch = false;")
            .append("      x = rows[i].getElementsByTagName('TD')[n];")
            .append("      y = rows[i + 1].getElementsByTagName('TD')[n];")
            .append("      if (dir == 'asc') {")
            .append("        if (x.innerHTML.toLowerCase() > y.innerHTML.toLowerCase()) {")
            .append("          shouldSwitch = true;")
            .append("          break;")
            .append("        }")
            .append("      } else if (dir == 'desc') {")
            .append("        if (x.innerHTML.toLowerCase() < y.innerHTML.toLowerCase()) {")
            .append("          shouldSwitch = true;")
            .append("          break;")
            .append("        }")
            .append("      }")
            .append("    }")
            .append("    if (shouldSwitch) {")
            .append("      rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);")
            .append("      switching = true;")
            .append("      switchcount++;")
            .append("    } else {")
            .append("      if (switchcount == 0 && dir == 'asc') {")
            .append("        dir = 'desc';")
            .append("        switching = true;")
            .append("      }")
            .append("    }")
            .append("  }")
            .append("}")
            .append("function updateCountdowns() {")
            .append("  var now = new Date().getTime();")
            .append("  var countdowns = document.getElementsByClassName('countdown');")
            .append("  for (var i = 0; i < countdowns.length; i++) {")
            .append("    var saleTime = new Date(countdowns[i].getAttribute('data-saletime')).getTime();")
            .append("    var distance = saleTime - now;")
            .append("    if (distance < 0) {")
            .append("      countdowns[i].innerHTML = '已开售';")
            .append("    } else {")
            .append("      var days = Math.floor(distance / (1000 * 60 * 60 * 24));")
            .append("      var hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));")
            .append("      var minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));")
            .append("      var seconds = Math.floor((distance % (1000 * 60)) / 1000);")
            .append("      countdowns[i].innerHTML = days + '天' + ")
            .append("        String(hours).padStart(2, '0') + ':' + ")
            .append("        String(minutes).padStart(2, '0') + ':' + ")
            .append("        String(seconds).padStart(2, '0');")
            .append("    }")
            .append("  }")
            .append("}")
            .append("setInterval(updateCountdowns, 1000);")
            .append("updateCountdowns();")
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

    @PostMapping("/{id}/update")
    public String updateProduct(@PathVariable String id, @RequestBody ProductForm form) {
        product.setId(id);
        product.updatePrice(form.getPrice());
        // 更新其他字段...
        return "Product updated successfully.";
    }
}