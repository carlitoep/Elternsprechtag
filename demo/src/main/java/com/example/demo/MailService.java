package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String toEmail, String token) {
        System.out.println("✅ MAILSERVICE AKTIV — versuche zu senden an: " + toEmail);
        String verifyUrl = "https://elternsprechtag-1.onrender.com/api/verify?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Bitte bestätige deine E-Mail-Adresse");
        msg.setText("Klicke hier, um zu bestätigen:\n" + verifyUrl);

        mailSender.send(msg);
    }
}
