package com.firstclub.membership;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.firstclub.membership.model.MembershipDuration;
import com.firstclub.membership.model.Plan;
import com.firstclub.membership.model.Subscription;
import com.firstclub.membership.model.SubscriptionStatus;
import com.firstclub.membership.model.User;
import com.firstclub.membership.repository.UserRepository;
import com.firstclub.membership.service.MembershipService;

@SpringBootTest
public class MembershipFlowTest {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testFullMembershipFlow() {
        // 1. Create User
        User user = new User(null, "Test User", "test@test.com", 0, 0.0, null);
        user = userRepository.save(user);
        Long userId = user.getId();

        // 2. Check initial plans (Should be Silver)
        List<Plan> plans = membershipService.getAvailablePlans(userId);
        Assertions.assertFalse(plans.isEmpty());
        // Since user is Silver, and minSpent <= Silver (0), they see Silver.
        // Gold has higher minSpent, so they shouldn't see Gold.
        Assertions.assertTrue(plans.stream().allMatch(p -> p.getTier().getName().equals("Silver")));

        // 3. Subscribe to Silver Monthly
        Plan silverPlan = plans.stream().filter(p -> p.getDuration() == MembershipDuration.MONTHLY).findFirst().get();
        Subscription sub = membershipService.subscribe(userId, silverPlan.getId());

        Assertions.assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        Assertions.assertEquals("Silver", sub.getPlan().getTier().getName());

        // 4. Simulate Orders to reach Gold (Needs 10 orders, $500)
        user.setTotalOrders(15);
        user.setTotalSpent(600.0);
        userRepository.save(user);

        // 5. Check plans again (Should see Gold plans now, AND Silver plans)
        List<Plan> goldPlans = membershipService.getAvailablePlans(userId);
        // Verify we can see Gold
        Assertions.assertTrue(goldPlans.stream().anyMatch(p -> p.getTier().getName().equals("Gold")));
        // Verify we can still see Silver (downgrade option)
        Assertions.assertTrue(goldPlans.stream().anyMatch(p -> p.getTier().getName().equals("Silver")));

        // 6. Upgrade to Gold
        Plan goldPlan = goldPlans.stream()
                .filter(p -> p.getTier().getName().equals("Gold") && p.getDuration() == MembershipDuration.MONTHLY)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Gold Monthly Plan not found"));

        Subscription upgradedSub = membershipService.subscribe(userId, goldPlan.getId());

        Assertions.assertEquals("Gold", upgradedSub.getPlan().getTier().getName());
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, upgradedSub.getStatus());
        Assertions.assertEquals(sub.getId(), upgradedSub.getId()); // Same Subscription ID (updated)

        // 7. Cancel
        membershipService.cancelSubscription(userId);
        Subscription cancelled = membershipService.getCurrentSubscription(userId);
        Assertions.assertEquals(SubscriptionStatus.CANCELLED, cancelled.getStatus());
    }
}
