package com.eshoppingzone.profile.config;

import com.eshoppingzone.profile.entity.Address;
import com.eshoppingzone.profile.entity.Role;
import com.eshoppingzone.profile.entity.UserProfile;
import com.eshoppingzone.profile.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initAdminUser(UserProfileRepository profileRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@eshoppingzone.com";
            if (!profileRepository.existsByEmail(adminEmail) && !profileRepository.existsByMobileNumber("9999999999")) {
                UserProfile admin = new UserProfile();
                admin.setFullName("System Admin");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setMobileNumber("9999999999");
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                admin.setAddress(new Address("1", "Admin Tower", "Central Zone", "New Delhi", "Delhi", "110001"));
                profileRepository.save(admin);
                log.info("Initialized default Administrator: {} / admin123", adminEmail);
            }

            String customerEmail = "customer@eshoppingzone.com";
            if (!profileRepository.existsByEmail(customerEmail) && !profileRepository.existsByMobileNumber("9800000001")) {
                UserProfile customer = new UserProfile();
                customer.setFullName("Ananya Sharma");
                customer.setEmail(customerEmail);
                customer.setPassword(passwordEncoder.encode("password"));
                customer.setMobileNumber("9800000001");
                customer.setRole(Role.CUSTOMER);
                customer.setActive(true);
                customer.setAddress(new Address("14", "Prestige Lavender", "Indiranagar", "Bengaluru", "Karnataka", "560038"));
                profileRepository.save(customer);
                log.info("Initialized default Customer: {} / password", customerEmail);
            }

            String merchantEmail = "merchant@eshoppingzone.com";
            if (!profileRepository.existsByEmail(merchantEmail) && !profileRepository.existsByMobileNumber("9812345678")) {
                UserProfile merchant = new UserProfile();
                merchant.setFullName("Prime Electronics");
                merchant.setEmail(merchantEmail);
                merchant.setPassword(passwordEncoder.encode("password"));
                merchant.setMobileNumber("9812345678");
                merchant.setRole(Role.MERCHANT);
                merchant.setActive(true);
                merchant.setAddress(new Address("45", "Tech Commerce Park", "Whitefield", "Bengaluru", "Karnataka", "560066"));
                profileRepository.save(merchant);
                log.info("Initialized default Merchant: {} / password", merchantEmail);
            }

            String agentEmail = "kavitha@eshoppingzone.com";
            if (!profileRepository.existsByEmail(agentEmail) && !profileRepository.existsByMobileNumber("9871122334")) {
                UserProfile agent = new UserProfile();
                agent.setFullName("Kavitha Delivery Specialist");
                agent.setEmail(agentEmail);
                agent.setPassword(passwordEncoder.encode("password"));
                agent.setMobileNumber("9871122334");
                agent.setRole(Role.DELIVERY_AGENT);
                agent.setActive(true);
                agent.setAddress(new Address("8", "Logistics Hub", "Koramangala", "Bengaluru", "Karnataka", "560034"));
                profileRepository.save(agent);
                log.info("Initialized default Delivery Agent: {} / password", agentEmail);
            }
        };
    }
}
