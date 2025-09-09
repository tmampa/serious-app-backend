package com.seriousapp.serious.app.contact;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {
    private final EmailRepository emailRepository;

    public EmailService(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    public Email saveEmail(Email email) {
        return emailRepository.save(email);
    }

    public Email findById(Long id) {
        return emailRepository.findById(id).orElse(null);
    }

}
