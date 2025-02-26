/*
 * Copyright (c) 2023 Ken Ng, Inc. All rights reserved.
 */

package com.knyc.opa;

//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

// Replace javax imports with jakarta
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;

import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class SMTPExtension {
    private static final Logger logger = LoggerFactory.getLogger(SMTPExtension.class);

    private Properties configureSMTPProperties(Map<String, Object> config) {
        Properties prop = new Properties();
        String authType = (String) config.getOrDefault("authType", "STARTTLS");
        
        // Basic properties
        prop.put("mail.smtp.host", (String) config.get("host"));
        
        // Handle port conversion
        int port;
        Object portValue = config.get("port");
        if (portValue instanceof String) {
            port = Integer.parseInt((String) portValue);
        } else if (portValue instanceof Integer) {
            port = (Integer) portValue;
        } else {
            throw new IllegalArgumentException("Port must be a number");
        }
        prop.put("mail.smtp.port", port);

        switch (authType.toUpperCase()) {
            case "NONE":
                prop.put("mail.smtp.auth", "false");
                break;
            case "TLS":
                prop.put("mail.smtp.auth", "true");
                prop.put("mail.smtp.starttls.enable", "true");
                prop.put("mail.smtp.starttls.required", "true");
                break;
            case "SSL":
                prop.put("mail.smtp.auth", "true");
                prop.put("mail.smtp.socketFactory.port", port);
                prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                prop.put("mail.smtp.ssl.enable", "true");
                break;
            case "STARTTLS":
                prop.put("mail.smtp.auth", "true");
                prop.put("mail.smtp.starttls.enable", "true");
                break;
            default:
                throw new IllegalArgumentException("Unsupported authentication type: " + authType);
        }

        // Handle timeout conversions
        if (config.containsKey("connectionTimeout")) {
            Object timeout = config.get("connectionTimeout");
            if (timeout instanceof String) {
                prop.put("mail.smtp.connectiontimeout", Integer.parseInt((String) timeout));
            } else if (timeout instanceof Integer) {
                prop.put("mail.smtp.connectiontimeout", timeout);
            }
        }
        if (config.containsKey("timeout")) {
            Object timeout = config.get("timeout");
            if (timeout instanceof String) {
                prop.put("mail.smtp.timeout", Integer.parseInt((String) timeout));
            } else if (timeout instanceof Integer) {
                prop.put("mail.smtp.timeout", timeout);
            }
        }

        // Add debug property for troubleshooting
        prop.put("mail.debug", "false");
        prop.put("mail.debug.auth", "false");
        
        return prop;
    }

    @RequestMapping(path = "/test", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> test(@RequestBody Map<String, Object> body) throws Exception {
        
        Map<String, Object> responseData = new HashMap<String, Object>();
        String errorMsg = "";

        String username = (String) body.get("username");
        String password = (String) body.get("password");

        try {
            Properties prop = configureSMTPProperties(body);
            Session session;
            
            if (prop.getProperty("mail.smtp.auth").equals("true")) {
                session = Session.getInstance(prop, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                session = Session.getInstance(prop);
            }

            // Update test message to use multipart for consistency
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(username));
            message.setSubject("Test Email from Workato SMTP Connector");

            // Create multipart message
            Multipart multipart = new MimeMultipart();
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("This is a test email to validate that the Workato SMTP Connector settings are set up correctly.");
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);

            logger.debug("Sending test email to {}", username);
            Transport.send(message);
            logger.info("Test email sent successfully to {}", username);
            
        } catch (Exception e) {
            logger.error("Error sending test email: {}", e.getMessage(), e);
            errorMsg = e.toString();
        }
        
        if (errorMsg.isEmpty()) {
            responseData.put("status","success");
            responseData.put("message","email sent successfully");
        } else {
            responseData.put("status","error");
            responseData.put("message",errorMsg);
        }        
        
        return responseData;
    }

    @RequestMapping(path = "/sendEmail", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> sendEmail(@RequestBody Map<String, Object> body) throws Exception {
        
        Map<String, Object> responseData = new HashMap<String, Object>();
        String from = (String) body.get("from");
        String recipients = (String) body.get("recipients");
        String subject = (String) body.get("subject");
        String emailBody = (String) body.get("emailBody");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> attachments = (List<Map<String, String>>) body.get("attachments");
        String emailMimeType = (String) body.get("emailMimeType");
        String errorMsg = "";

        String username = (String) body.get("username");
        String password = (String) body.get("password");

        try {
            Properties prop = configureSMTPProperties(body);
            Session session;
            
            if (prop.getProperty("mail.smtp.auth").equals("true")) {
                session = Session.getInstance(prop, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                session = Session.getInstance(prop);
            }

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(recipients));
            message.setSubject(subject);

            // Create multipart message
            Multipart multipart = new MimeMultipart();

            // Add body part with proper content type
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(emailBody, emailMimeType != null ? emailMimeType : "text/plain; charset=UTF-8");
            multipart.addBodyPart(messageBodyPart);

            // Add attachments if present
            if (attachments != null && !attachments.isEmpty()) {
                for (Map<String, String> attachment : attachments) {
                    try {
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        String base64Content = attachment.get("content");
                        if (base64Content == null || base64Content.isEmpty()) {
                            throw new IllegalArgumentException("Attachment content cannot be empty");
                        }
                        
                        byte[] decodedFile = Base64.getDecoder().decode(base64Content);
                        String contentType = attachment.getOrDefault("contentType", "application/octet-stream");
                        String filename = attachment.getOrDefault("filename", "attachment");
                        
                        DataSource source = new ByteArrayDataSource(decodedFile, contentType);
                        attachmentPart.setDataHandler(new DataHandler(source));
                        attachmentPart.setFileName(filename);
                        multipart.addBodyPart(attachmentPart);
                        
                        logger.debug("Added attachment: {}, type: {}", filename, contentType);
                    } catch (Exception e) {
                        logger.error("Error adding attachment: {}", e.getMessage(), e);
                        throw new RuntimeException("Error processing attachment: " + e.getMessage(), e);
                    }
                }
            }

            message.setContent(multipart);
            logger.debug("Sending email to {} with {} attachments", recipients, 
                        attachments != null ? attachments.size() : 0);
            Transport.send(message);
            logger.info("Email sent successfully to {}", recipients);
            
        } catch (Exception e) {
            logger.error("Error sending email: {}", e.getMessage(), e);
            errorMsg = e.toString();
        }
        
        if (errorMsg.isEmpty()) {
            responseData.put("status","success");
            responseData.put("message","email sent successfully");
        } else {
            responseData.put("status","error");
            responseData.put("message",errorMsg);
        }        
        
        return responseData;
    }

}
