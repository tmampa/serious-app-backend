package com.seriousapp.serious.app.parent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ParentService {
    private final ParentRepository parentRepository;

    public ParentService(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    public Parent saveEmail(Parent parent) {
        return parentRepository.save(parent);
    }

    public Parent findById(Long id) {
        return parentRepository.findById(id).orElse(null);
    }

}
