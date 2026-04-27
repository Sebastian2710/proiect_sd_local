package com.andrei.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Admin1234!");
        System.out.println("==================");
        System.out.println(hash);
        System.out.println("==================");
    }
}