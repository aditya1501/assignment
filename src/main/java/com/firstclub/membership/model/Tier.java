package com.firstclub.membership.model;

import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // Silver, Gold, Platinum

    // Qualification Criteria
    private int minOrderCount;
    private double minTotalSpent;
    private String requiredCohort; // e.g. "STUDENT", "VIP", or null/empty for all

    // Configurable Benefits (e.g., "DISCOUNT" -> "5%", "DELIVERY" -> "FREE")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tier_benefits", joinColumns = @JoinColumn(name = "tier_id"))
    @MapKeyColumn(name = "benefit_name")
    @Column(name = "benefit_value")
    private Map<String, String> benefits;
}
