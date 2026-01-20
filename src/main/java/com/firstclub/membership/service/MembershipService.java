package com.firstclub.membership.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.firstclub.membership.model.MembershipDuration;
import com.firstclub.membership.model.Plan;
import com.firstclub.membership.model.Subscription;
import com.firstclub.membership.model.SubscriptionStatus;
import com.firstclub.membership.model.Tier;
import com.firstclub.membership.model.User;
import com.firstclub.membership.repository.PlanRepository;
import com.firstclub.membership.repository.SubscriptionRepository;
import com.firstclub.membership.repository.TierRepository;
import com.firstclub.membership.repository.UserRepository;

@Service
public class MembershipService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private TierRepository tierRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    /**
     * Get all plans available for the user based on their eligibility.
     */
    public List<Plan> getAvailablePlans(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Determine eligible tier
        Tier eligibleTier = calculateEligibleTier(user);

        // Logic: A user qualifies for a Tier.
        // We return plans for the eligible tier AND any lower tiers (Downgrade
        // options).
        return planRepository.findAll().stream()
                .filter(p -> p.getTier().getMinTotalSpent() <= eligibleTier.getMinTotalSpent())
                .collect(Collectors.toList());
    }

    /**
     * Subscribe a user to a specific plan.
     * Handles Upgrade/Downgrade if a subscription already exists.
     */
    @Transactional
    public Subscription subscribe(Long userId, Long planId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        // Validate eligibility (Double check)
        Tier eligibleTier = calculateEligibleTier(user);

        // If the user is trying to subscribe to a higher tier than eligible, throw
        // error?
        // Rules say "Users move through tiers... based on criteria".
        // Assuming strict rule: You can only subscribe to your earned tier.
        if (!plan.getTier().getName().equals(eligibleTier.getName())) {
            // Exception: Maybe they can DOWNGRADE?
            // If eligible for Gold, can I buy Silver? Usually yes.
            // If eligible for Silver, can I buy Gold? No.

            // Let's implement logic: Eligible Tier (e.g. Gold) >= Plan Tier (Silver).
            // We need a weight/sequence for Tiers.
            // For simplicity, let's look up all Tiers and compare order.
            // Assume ID order implies rank or add a 'rank' field.
            // For now, let's strictly enforce: You subscribe to the Tier you belong to.
            // (Or allow downgrade logic if needed, but Prompt says "Upgrade,
            // downgrade...").
            // If I am Gold, I might want to pay less and be Silver?
            // Let's allow if eligibleTier rank >= planTier rank.
            // For this Assignment, let's start strict: You get the plan for your designated
            // Tier.
            // Actually "Upgrade, downgrade(Membership Tier)" is a user action.
            // This implies I *can* choose.
            // So: Eligible Check: can I buy this tier?
            if (isTierHigher(plan.getTier(), eligibleTier)) {
                throw new RuntimeException("User not eligible for this Tier yet.");
            }
        }

        Optional<Subscription> existingSubOpt = subscriptionRepository.findByUser(user);

        Subscription subscription;
        if (existingSubOpt.isPresent()) {
            // Upgrade/Downgrade/Renewal
            subscription = existingSubOpt.get();
            // If active, we might adjust endDate or refund?
            // "Upgrade, downgrade...".
            // Simplest logic: End current, start new immediately.
            subscription.setStatus(SubscriptionStatus.CANCELLED); // Or just overwrite
            // Actually, keep the same object, update plan and dates.
            subscription.setPlan(plan);
            subscription.setStartDate(LocalDate.now());
            subscription.setEndDate(calculateEndDate(LocalDate.now(), plan.getDuration()));
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        } else {
            subscription = new Subscription();
            subscription.setUser(user);
            subscription.setPlan(plan);
            subscription.setStartDate(LocalDate.now());
            subscription.setEndDate(calculateEndDate(LocalDate.now(), plan.getDuration()));
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void cancelSubscription(Long userId) {
        Subscription sub = subscriptionRepository.findByUser(userRepository.getReferenceById(userId))
                .orElseThrow(() -> new RuntimeException("No subscription found"));
        sub.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(sub);
    }

    public Subscription getCurrentSubscription(Long userId) {
        return subscriptionRepository.findByUser(userRepository.getReferenceById(userId))
                .orElse(null);
    }

    /**
     * Determines user's eligible tier based on stats.
     */
    public Tier calculateEligibleTier(User user) {
        List<Tier> allTiers = tierRepository.findAll(); // Should be cached ideally
        // Sort by criteria (highest first) to find best match
        // Assuming higher ID = higher tier or use explicit rank.
        // Let's assume explicit rank logic: Platinum > Gold > Silver
        // We will infer order by criteria values.

        Tier bestTier = null;
        for (Tier t : allTiers) {
            // Logic: User must meet BOTH or ANY? "Number of Order ... OR Total Order
            // value".
            // Prompt: "based on criteria like: Number of Order more than X, Total Order
            // value..."
            // Usually criteria are thresholds.
            // Let's assume "OR" for flexibility, or "AND". Let's do AND for stricter, or OR
            // for user friendly.
            // Criteria check: Stats + Cohort
            boolean statsMet = user.getTotalOrders() >= t.getMinOrderCount()
                    && user.getTotalSpent() >= t.getMinTotalSpent();
            boolean cohortMet = t.getRequiredCohort() == null || t.getRequiredCohort().isEmpty()
                    || t.getRequiredCohort().equalsIgnoreCase(user.getCohort());

            if (statsMet && cohortMet) {
                if (bestTier == null || isTierHigher(t, bestTier)) {
                    bestTier = t;
                }
            }
        }

        // Default to lowest if none (should have a 'Base' tier? or just return Silver
        // as default)
        if (bestTier == null) {
            return allTiers.stream().filter(t -> t.getName().equalsIgnoreCase("Silver")).findFirst()
                    .orElseThrow(() -> new RuntimeException("Base Silver tier not found"));
        }
        return bestTier;
    }

    private LocalDate calculateEndDate(LocalDate start, MembershipDuration duration) {
        switch (duration) {
            case MONTHLY:
                return start.plusMonths(1);
            case QUARTERLY:
                return start.plusMonths(3);
            case YEARLY:
                return start.plusYears(1);
            default:
                return start.plusMonths(1);
        }
    }

    private boolean isTierHigher(Tier t1, Tier t2) {
        // Simple comparison: Metric based.
        // If t1 requires more spend than t2, it is higher.
        return t1.getMinTotalSpent() > t2.getMinTotalSpent();
    }
}
