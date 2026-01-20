# Membership Program Logic & Flow

## 1. Tier Eligibility & Plan Selection Logic
This flow shows how a User's stats determine their eligible Tier and which Plans they can view/purchase.

```mermaid
flowchart TD
    User((User)) -->|1. Request Available Plans| API["API: /plans/{userId}"]
    API -->|Fetch User Stats| DB[(Database)]
    DB -->|Return Orders, Spend, Cohort| Service[Membership Service]
    
    subgraph Tier Calculation
        Service -->|Iterate All Tiers| Criteria{Check Criteria}
        Criteria -->|Stats >= Min AND Cohort Matches| Eligible[Mark as Eligible]
        Criteria -->|Stats < Min| NotEligible[Ignore]
        Eligible -->|Determine Highest Rank| BestTier[Set Eligible Tier]
    end
    
    BestTier -->|Filter Plans| PlanFilter{Plan Filter}
    PlanFilter -->|Plan Tier <= Eligible Tier| Show[Add to List]
    PlanFilter -->|Plan Tier > Eligible Tier| Hide[Exclude]
    
    Show -->|Return List| User
    
    style BestTier fill:#f9f,stroke:#333
    style Eligible fill:#dfd,stroke:#333
```

## 2. Subscription Lifecycle
This state diagram shows the lifecycle of a subscription including Upgrades and Downgrades.

```mermaid
stateDiagram-v2
    [*] --> NoSubscription
    
    NoSubscription --> Active: Subscribe (New)
    
    state Active {
        [*] --> CurrentPlan
        CurrentPlan --> Cancelled: User Cancels
        CurrentPlan --> Expired: Date > EndDate
        CurrentPlan --> Upgraded: User Selects Higher Tier Plan
        CurrentPlan --> Downgraded: User Selects Lower Tier Plan
    }

    Upgraded --> Active: New Plan Starts (Immediate)
    Downgraded --> Active: New Plan Starts (Immediate)
    
    Cancelled --> [*]
    Expired --> NoSubscription
```

## 3. Detailed Subscription Flow
Sequence of events when a user subscribes.

```mermaid
sequenceDiagram
    actor User
    participant Controller
    participant Service
    participant Repo as Repository
    
    User->>Controller: POST /subscribe (userId, planId)
    Controller->>Service: subscribe(userId, planId)
    Service->>Repo: findUser(userId), findPlan(planId)
    
    rect rgb(240, 240, 240)
        Note left of Service: Eligibility Check
        Service->>Service: calculateEligibleTier(User)
        Service->>Service: Verify Plan Tier <= Eligible Tier
        alt Not Eligible
            Service-->>User: Error: Not Eligible for this Tier
        end
    end
    
    Service->>Repo: findActiveSubscription(user)
    alt Has Active Subscription
        Note right of Service: Upgrade / Downgrade
        Service->>Repo: Update Old Sub -> CANCELLED
        Service->>Repo: Create New Sub -> ACTIVE
    else No Subscription
        Service->>Repo: Create New Sub -> ACTIVE
    end
    
    Service-->>User: Return Subscription Details
```
