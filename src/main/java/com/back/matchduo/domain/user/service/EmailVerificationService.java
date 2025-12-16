package com.back.matchduo.domain.user.service;

import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {
    public boolean verifyCode(String email, String code) {
        return true;
    }
}
