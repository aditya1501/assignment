package com.firstclub.membership.config;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.firstclub.membership.model.MembershipDuration;
import com.firstclub.membership.model.Plan;
import com.firstclub.membership.model.Tier;
import com.firstclub.membership.model.User;
import com.firstclub.membership.repository.PlanRepository;
import com.firstclub.membership.repository.TierRepository;
import com.firstclub.membership.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(TierRepository tierRepo, PlanRepository planRepo, UserRepository userRepo) {
        return args -> {
            // Create Tiers
            Tier silver = new Tier(null, "Silver", 0, 0.0, Map.of("DISCOUNT", "5%", "DELIVERY", "STANDARD"));
            Tier gold = new Tier(null, "Gold", 10, 500.0, Map.of("DISCOUNT", "10%", "DELIVERY", "FREE_ON_ELIGIBLE"));
            Tier platinum = new Tier(null, "Platinum", 50, 2000.0,
                    Map.of("DISCOUNT", "20%", "DELIVERY", "FREE_ALL", "SUPPORT", "PRIORITY"));

            tierRepo.save(silver);
            tierRepo.save(gold);
            tierRepo.save(platinum);

            // Create Plans
            // Silver
            planRepo.save(new Plan(null, silver, MembershipDuration.MONTHLY, new BigDecimal("9.99")));
            planRepo.save(new Plan(null, silver, MembershipDuration.YEARLY, new BigDecimal("99.99")));

            // Gold
            planRepo.save(new Plan(null, gold, MembershipDuration.MONTHLY, new BigDecimal("19.99")));
            planRepo.save(new Plan(null, gold, MembershipDuration.QUARTERLY, new BigDecimal("55.00")));
            planRepo.save(new Plan(null, gold, MembershipDuration.YEARLY, new BigDecimal("199.99")));

            // Platinum
            planRepo.save(new Plan(null, platinum, MembershipDuration.MONTHLY, new BigDecimal("49.99")));
            planRepo.save(new Plan(null, platinum, MembershipDuration.YEARLY, new BigDecimal("499.99")));

            // Create Demo User
            User user = new User(null, "John Doe", "john@example.com", 0, 0.0);
            userRepo.save(user); // ID 1

            System.out.println("Database initialized with Tiers, Plans, and Demo User.");
        };
    }
}
