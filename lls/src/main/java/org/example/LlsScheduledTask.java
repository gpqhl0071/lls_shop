package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LlsScheduledTask {
  private final Lls llsTask;
  private final ProductFinder productFinder;
  private final EmailService emailService;

  @Autowired
  public LlsScheduledTask(Lls llsTask, EmailService emailService, ProductFinder productFinder) {
    this.llsTask = llsTask;
    this.emailService = emailService;
    this.productFinder = productFinder;
  }

  @Scheduled(fixedRate = 60000) // 每60秒执行一次
  public void scheduledTask() {
    llsTask.fetchAndPrintProductInfo(false);
  }

  @Scheduled(fixedRate = 120000) // 每60秒执行一次
  public void scheduledTask1() {
    productFinder.handle();
  }


}
