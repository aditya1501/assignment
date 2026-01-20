package com.firstclub.membership.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.firstclub.membership.model.MembershipDuration;
import com.firstclub.membership.model.Plan;
import com.firstclub.membership.model.Tier;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByTierAndDuration(Tier tier, MembershipDuration duration);
}
