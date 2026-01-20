package com.firstclub.membership.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.firstclub.membership.model.Tier;

@Repository
public interface TierRepository extends JpaRepository<Tier, Long> {
    Optional<Tier> findByName(String name);
}
