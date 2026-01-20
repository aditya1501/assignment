package com.firstclub.membership.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.firstclub.membership.model.Plan;
import com.firstclub.membership.model.Subscription;
import com.firstclub.membership.model.User;
import com.firstclub.membership.repository.UserRepository;
import com.firstclub.membership.service.MembershipService;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/plans/{userId}")
    public List<Plan> getAvailablePlans(@PathVariable Long userId) {
        return membershipService.getAvailablePlans(userId);
    }

    @PostMapping("/subscribe")
    public Subscription subscribe(@RequestParam Long userId, @RequestParam Long planId) {
        return membershipService.subscribe(userId, planId);
    }

    @PostMapping("/cancel")
    public void cancel(@RequestParam Long userId) {
        membershipService.cancelSubscription(userId);
    }

    @GetMapping("/current/{userId}")
    public Subscription getCurrentSubscription(@PathVariable Long userId) {
        return membershipService.getCurrentSubscription(userId);
    }

    // Helper to simulate order and increase stats
    @PostMapping("/simulate-order")
    public User simulateOrder(@RequestParam Long userId, @RequestParam double amount) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setTotalOrders(user.getTotalOrders() + 1);
        user.setTotalSpent(user.getTotalSpent() + amount);
        return userRepository.save(user);
    }
}
