package com.eshoppingzone.website;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class WebsiteControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebsiteControllerApplication.class, args);
    }
}
