//package org.example;
//
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.annotation.Bean;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@SpringBootApplication
//@RestController
//@EnableScheduling
//public class Main {
//  public static void main(String[] args) {
//    SpringApplication.run(Main.class, args);
//  }
//
//  @Bean
//  public CommandLineRunner commandLineRunner() {
//    return args -> {
//      System.out.println("Spring Boot应用已成功启动!");
//    };
//  }
//
//  @GetMapping("/hello")
//  public String hello() {
//    return "Hello, Spring Boot!";
//  }
//}