package com.homehn.backend.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class pass {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("Password@123"));
    }
}
