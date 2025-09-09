package com.seriousapp.serious.app.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import com.azure.communication.email.*;
import com.azure.communication.email.models.*;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;

import java.util.List;

@Component
@Slf4j
public class EmailConfiguration {
    @Value("${azure.email.key}")
    private String key;
    @Value("${azure.email.connection.string}")
    private String connectionString;

    @Bean
    public EmailClient createEmailClient() {
        return new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    public void sendEmail(List<String> emails, String subject, String bodyPlainText, String bodyHtml) {
        EmailClient emailClient = createEmailClient();
        // Map emails to EmailAddress objects
        List<EmailAddress> toAddresses = emails.stream()
                .map(EmailAddress::new)
                .toList();

        EmailMessage emailMessage = new EmailMessage()
                .setSenderAddress("DoNotReply@6393192f-7be1-4d41-b464-0743bbeb3239.azurecomm.net")  // Replace XXXXX with your actual resource name
                .setToRecipients(toAddresses)
                .setSubject(subject)
                .setBodyPlainText(bodyPlainText)
                .setBodyHtml(bodyHtml);

        SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(emailMessage, null);
        PollResponse<EmailSendResult> result = poller.waitForCompletion();
        log.info("Email send status: {}", result.getStatus());
    }

}
