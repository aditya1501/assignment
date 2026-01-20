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
        User user = new User(null, "Test User", "test@test.com", 0, 0.0);
        user = userRepository.save(user);
        Long userId = user.getId();

        // 2. Check initial plans (Should be Silver)
        List<Plan> plans = membershipService.getAvailablePlans(userId);
        Assertions.assertFalse(plans.isEmpty());
        Assertions.assertTrue(plans.stream().allMatch(p -> p.getTier().getName().equals("Silver")));

        // 3. Subscribe to Silver Monthly
        Plan silverPlan = plans.stream().filter(p -> p.getDuration() == MembershipDuration.MONTHLY).findFirst().get();
        Subscription sub = membershipService.subscribe(userId, silverPlan.getId());

        Assertions.assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        Assertions.assertEquals("Silver", sub.getPlan().getTier().getName());

        // 4. Simulate Orders to reach Gold (Needs 10 orders, $500)
        // We update entity directly for test speed
        user.setTotalOrders(15);
        user.setTotalSpent(600.0);
        userRepository.save(user);

        // 5. Check plans again (Should see Gold plans now)
        // NOTE: My implementation implementation filters plans.
        // If I am Gold eligible, do I see Silver AND Gold?
        // My implementation: `return plans matching the eligible tier`.
        // logic: `filter(p -> p.getTier().getId().equals(eligibleTier.getId()))`
        // So I will see ONLY Gold. This adheres to "Subscribe to a plan (plan + tier)"
        // where tier is determined by criteria.
        // If users can *choose* to stay on Silver, my logic is too strict.
        // But for this assignment, simpler is often better to prove the "Criteria"
        // logic works.
        // Let's verify I see Gold.
        List<Plan> goldPlans = membershipService.getAvailablePlans(userId);
        Assertions.assertTrue(goldPlans.stream().anyMatch(p -> p.getTier().getName().equals("Gold")));
        // My logic returns *only* eligible tier. So it should be Gold.
        Assertions.assertEquals("Gold", goldPlans.get(0).getTier().getName()); // assuming first is Gold

        // 6. Upgrade to Gold
        Plan goldPlan = goldPlans.stream().filter(p -> p.getDuration() == MembershipDuration.MONTHLY).findFirst().get();
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
