package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Lls {
  private static final String API_URL = "https://plat-deal-api.lilithgame.com/api/v1/products?page=1&page_size=200&order_by=1&is_desc=true&status=3&game_id=10043&ltp_env_id=official_cn";
  private static final Map<String, Double> initialPrices = new ConcurrentHashMap<>();

  @Autowired
  private EmailService emailService;

  public void fetchAndPrintProductInfo(boolean isInitialLoad) {
    HttpURLConnection connection = null;
    BufferedReader reader = null;
    StringBuilder emailContent = new StringBuilder();
    boolean shouldSendEmail = false;

    try {
      URL url = new URL(API_URL);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");

      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = reader.readLine()) != null) {
          response.append(inputLine);
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = mapper.readValue(response.toString(), Map.class);

        if ((Integer) jsonMap.get("code") == 0) {
          Map<String, Object> data = (Map<String, Object>) jsonMap.get("data");
          List<Map<String, Object>> productList = (List<Map<String, Object>>) data.get("product_list");

          Collections.sort(productList, (p1, p2) -> {
            Double price1 = Double.parseDouble(p1.get("price").toString());
            Double price2 = Double.parseDouble(p2.get("price").toString());
            return price1.compareTo(price2);
          });

          for (Map<String, Object> product : productList) {
            double priceInYuan = Double.parseDouble(product.get("price").toString()) / 100.0;
            String flagId = product.get("flag_id").toString();

            if (isInitialLoad) {
              initialPrices.put(flagId, priceInYuan);
              printProductInfo(product, priceInYuan);
            } else {
              if (initialPrices.containsKey(flagId)) {
                double oldPrice = initialPrices.get(flagId);
                if (priceInYuan - oldPrice >= 500 || (priceInYuan <= 300 && priceInYuan != oldPrice)) {
                  shouldSendEmail = true;
                  String remark = String.format("价格变化 - Flag ID: %s, 之前价格: %.2f, 当前价格: %.2f%n", flagId, oldPrice, priceInYuan);
                  System.out.println(remark);
                  System.out.println(String.format("https://trade.lilith.com/detail/%s ", flagId));
                  initialPrices.put(flagId, priceInYuan); // 更新价格
                  printProductInfo(product, priceInYuan);
                  appendProductInfoToEmail(emailContent, product, priceInYuan, remark);
                }
              } else if (priceInYuan <= 1000) {
                shouldSendEmail = true;
                String remark = String.format("新增记录 - Flag ID: %s, 价格: %.2f %s%n", flagId, priceInYuan, product.get("currency"));
                System.out.println(remark);
                System.out.println(String.format("https://trade.lilith.com/detail/%s ", flagId));
                initialPrices.put(flagId, priceInYuan); // 添加新记录
                printProductInfo(product, priceInYuan);
                appendProductInfoToEmail(emailContent, product, priceInYuan, remark);
              }
            }
          }

          if (!isInitialLoad && !shouldSendEmail) {
            System.out.println("价格无变化");
          }
        } else {
          System.out.println("请求失败，错误码: " + jsonMap.get("code"));
        }
      } else {
        System.out.println("HTTP请求失败，响应码: " + responseCode);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      if (connection != null) {
        connection.disconnect();
      }
    }

    if (shouldSendEmail) {
      emailService.sendEmail("gpqhl0071@126.com", "价格监控报告", emailContent.toString());
    }
  }

  private void printProductInfo(Map<String, Object> product, double priceInYuan) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> introduction = mapper.readValue(product.get("introduction").toString(), Map.class);
    Map<String, Object> detail = (Map<String, Object>) introduction.get("detail");
    List<String> awakedHeroes = (List<String>) detail.get("awaked_heroes");

    System.out.printf("Flag ID: %s, 价格: %.2f %s, VIP: %s, 觉醒英雄数: %d%n",
        product.get("flag_id"),
        priceInYuan,
        product.get("currency"),
        detail.get("vip"),
        awakedHeroes.size());

    System.out.println("觉醒英雄: " + joinStrings(awakedHeroes, ", "));
    System.out.println("--------------------");
  }

  private void appendProductInfoToEmail(StringBuilder emailContent, Map<String, Object> product, double priceInYuan, String remark) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> introduction = mapper.readValue(product.get("introduction").toString(), Map.class);
      Map<String, Object> detail = (Map<String, Object>) introduction.get("detail");
      List<String> awakedHeroes = (List<String>) detail.get("awaked_heroes");
      String flagId = product.get("flag_id").toString();

      emailContent.append("<tr><td style='border: 1px solid #ddd; padding: 8px;'>");

      // 添加remark,用醒目的样式包裹
      emailContent.append(String.format("<strong style='color: #FF4500; display: block; margin-bottom: 5px;'>%s</strong>", remark));

      emailContent.append(String.format("Flag ID: %s<br>", flagId));
      emailContent.append(String.format("价格: %.2f %s<br>", priceInYuan, product.get("currency")));
      emailContent.append(String.format("VIP: %s<br>", detail.get("vip")));
      emailContent.append(String.format("觉醒英雄数: %d<br>", awakedHeroes.size()));
      emailContent.append("觉醒英雄: ").append(joinStrings(awakedHeroes, ", ")).append("<br>");

      // 添加产品详情页链接
      String productUrl = String.format("https://trade.lilith.com/detail/%s", flagId);
      emailContent.append(String.format("<a href='%s'>查看详情</a><br>", productUrl));

      emailContent.append("</td></tr>");
    } catch (Exception e) {
      emailContent.append("<tr><td style='border: 1px solid #ddd; padding: 8px;'>");
      emailContent.append("解析产品详情时出错: ").append(e.getMessage());
      emailContent.append("</td></tr>");
    }
  }

  private String joinStrings(List<String> strings, String delimiter) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < strings.size(); i++) {
      result.append(strings.get(i));
      if (i < strings.size() - 1) {
        result.append(delimiter);
      }
    }
    return result.toString();
  }
}