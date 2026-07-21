package com.sky;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(properties = {
    "sky.oauth2.google.client-id=test-client-id",
    "sky.oauth2.google.client-secret=test-client-secret"
})
class PasswordEncoderTests {

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Value("${sky.employee-default-password}")
    private String employeeDefaultPassword;

    @Disabled("Only run manually")
    @Test
    void passwordEncoderTest() {

        String password = passwordEncoder.encode(employeeDefaultPassword);
        System.out.println(password);
        assertTrue(passwordEncoder.matches(employeeDefaultPassword, password));

    }
}
