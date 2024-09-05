package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Service
public class EmailService {
  @Autowired
  private JavaMailSender mailSender;

  public void sendEmail(String to, String subject, String content) {

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom("17334977799@163.com");
      helper.setTo(to);
      helper.setSubject(subject);

      String htmlContent = "<html><body>"
          + "<table style='width: 100%; max-width: 600px; border-collapse: collapse;'>"
          + "<tr><th style='text-align: left; padding: 10px; background-color: #f2f2f2;'>详情</th></tr>"
          + content
          + "</table>"
          + "</body></html>";

      helper.setText(htmlContent, true);

      mailSender.send(message);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
