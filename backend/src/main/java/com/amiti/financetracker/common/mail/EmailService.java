package com.amiti.financetracker.common.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail-from:no-reply@amiti.local}")
    private String from;

    @Value("${spring.mail.host:}")
    private String host;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String bodyText) {
        if (host == null || host.isBlank()) {
            log.warn("SMTP is not configured. Email to {} with subject '{}' was not sent.", to, subject);
            throw new IllegalStateException("Email delivery is not configured");
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(bodyText, false);
            mailSender.send(message);
        } catch (MessagingException ex) {
            log.error("Failed to compose email for {}", to, ex);
            throw new IllegalStateException("Email compose failed");
        } catch (RuntimeException ex) {
            log.error("Failed to send email to {}", to, ex);
            throw ex;
        }
    }
}
