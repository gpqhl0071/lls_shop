package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@SpringBootApplication
public class ProductFinderApplication implements CommandLineRunner {

  @Autowired
  private ProductFinder productFinder;
  @Autowired
  private EmailService emailService;

  public static void main(String[] args) {
    SpringApplication.run(ProductFinderApplication.class, args);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Override
  public void run(String... args) {
    testProductFinder();
  }

  /**
   * 测试方法，用于调用 ProductFinder 并打印结果
   */
  private void testProductFinder() {
    //System.out.println("开始测试 ProductFinder...");
    //
    //int hoursThreshold = 1; // 1小时内开售
    //double priceThreshold = 500; // 价格低于500元
    //
    //List<ProductFinder.Product> products = productFinder.findUpcomingCheapProducts(hoursThreshold, priceThreshold);
    //
    //String htmlReport = generateHtmlReport(products);
    //System.out.println(htmlReport);
    //
    //// 这里您可以添加发送邮件的代码
    //emailService.sendEmail("gpqhl0071@126.com", "价格监控报告", htmlReport);
  }

}