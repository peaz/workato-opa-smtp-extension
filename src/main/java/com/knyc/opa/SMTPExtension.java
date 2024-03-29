/*
 * Copyright (c) 2023 Ken Ng, Inc. All rights reserved.
 */

package com.knyc.opa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SMTPExtension {

    @Autowired
    private Environment env;

    @RequestMapping(path = "/sendEmail", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> sendEmail(@RequestBody Map<String, Object> body) throws Exception {
        
        Map<String, Object> responseData = new HashMap<String, Object>();
        String from = (String) body.get("from");
        String recipients = (String) body.get("recipients");
        String subject = (String) body.get("subject");
        String emailBody = (String) body.get("emailBody");
        //String emailMimeType = (String) body.get("emailMimeType");
        String errorMsg = "";

        String username = env.getProperty("username");
        String password = env.getProperty("password");

        Properties prop = new Properties();
            prop.put("mail.smtp.auth", "true");
            prop.put("mail.smtp.starttls.enable", "true");
            prop.put("mail.smtp.host", env.getProperty("host"));
            prop.put("mail.smtp.port", Integer.parseInt(env.getProperty("port")));
        
        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(recipients));
            message.setSubject(subject);
            message.setText(emailBody);

            Transport.send(message);
        } catch (Exception e) {
            errorMsg = e.toString();
            e.printStackTrace();
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
