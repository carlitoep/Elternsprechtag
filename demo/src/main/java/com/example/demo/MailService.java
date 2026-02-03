package com.example.demo;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.sender.email}")
    private String senderEmail;

    @Value("${sendgrid.sender.name}")
    private String senderName;

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = "https://elternsprechtag-1.onrender.com/api/verify?token=" + token;

        Email from = new Email(senderEmail, senderName);
        Email to = new Email(toEmail);
        String subject = "Bitte bestätige deine E-Mail-Adresse";
        Content content = new Content("text/plain", "Klicke hier, um zu bestätigen:\n" + verifyUrl);

        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            System.out.println("✅ Mail gesendet an: " + toEmail + " | Status: " + response.getStatusCode());
        } catch (Exception e) {
            System.err.println("❌ Mailversand fehlgeschlagen an: " + toEmail);
            e.printStackTrace();
        }
    }
}
