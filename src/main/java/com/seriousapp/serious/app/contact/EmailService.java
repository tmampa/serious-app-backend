package com.seriousapp.serious.app.contact;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void deleteEmail(Email email) {
        emailRepository.delete(email);
    }

    public Email findById(Long id) {
        return emailRepository.findById(id).orElse(null);
    }

    public List<Email> findAllByStudentId(Long studentId) {
        return emailRepository.findAll().stream()
                .filter(email -> email.getStudent() != null && email.getStudent().getId().equals(studentId))
                .toList();
    }

    public List<Email> findAll() {
        return emailRepository.findAll();
    }
}
