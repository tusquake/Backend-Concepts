# Deployment Strategies Used by Big Tech Companies

A comprehensive guide to modern deployment strategies with real-world examples from industry leaders.

---

## Table of Contents
- [Overview](#overview)
- [Deployment Strategies](#deployment-strategies)
    - [Blue-Green Deployment](#1-blue-green-deployment)
    - [Canary Deployment](#2-canary-deployment)
    - [Rolling Deployment](#3-rolling-deployment)
    - [Feature Flags/Toggles](#4-feature-flagstoggles)
    - [Shadow Deployment](#5-shadow-deployment)
    - [Recreate Deployment](#6-recreate-deployment)
- [Which Companies Use What](#which-companies-use-which-strategy)
- [Best Practices](#best-practices)
- [Comparison Table](#comparison-table)

---

## Overview

Modern tech companies deploy code multiple times per day while maintaining 99.99% uptime. This guide explains the strategies they use to achieve zero-downtime deployments.

---

## Deployment Strategies

### 1. Blue-Green Deployment

**What it is:** Two identical production environments. One serves traffic (Blue), the other is idle (Green). Deploy to Green, test, then switch traffic.

**How it works:**
```
Step 1: Blue (v1.0) → Serving 100% traffic
        Green (idle) → Deploy v2.0

Step 2: Test Green thoroughly

Step 3: Switch traffic → Green (v2.0) now serves 100%

Step 4: Blue (v1.0) kept ready for instant rollback
```

**Pros:**
- Instant rollback capability
- Zero downtime
- Test in production-like environment

**Cons:**
- Requires 2x infrastructure
- Database migrations can be complex
- Expensive for small teams

**Real-world example:**
> Netflix deploys their streaming service updates using blue-green. They can switch between versions in seconds if issues arise during peak viewing hours.

---

### 2. Canary Deployment

**What it is:** Release new version to a small subset of users (5-10%) first, monitor, then gradually increase.

**How it works:**
```
Step 1: Deploy v2.0 to 5% of servers
Step 2: Monitor metrics (errors, latency, user behavior)
Step 3: If stable → increase to 25%
Step 4: If stable → increase to 50%
Step 5: If stable → increase to 100%
Step 6: If issues → rollback affected servers
```

**Pros:**
- Minimizes blast radius of bugs
- Real-world testing with actual users
- Gradual confidence building

**Cons:**
- Slower rollout process
- Complex traffic routing required
- Two versions running simultaneously

**Real-world example:**
> Facebook tests new News Feed algorithms on 1% of users in specific regions. If engagement metrics improve and no crashes occur, they expand to more users.

---

### 3. Rolling Deployment

**What it is:** Update servers one by one (or in small batches) while keeping the service running.

**How it works:**
```
100 servers running v1.0

Step 1: Update servers 1-10 → v2.0 (90 still on v1.0)
Step 2: Update servers 11-20 → v2.0 (80 still on v1.0)
...
Step 10: Update servers 91-100 → v2.0 (all updated)
```

**Pros:**
- No downtime
- No extra infrastructure needed
- Easy to implement

**Cons:**
- Slower rollback (must roll back each server)
- Two versions running during deployment
- Not suitable for breaking changes

**Real-world example:**
> Amazon Web Services (AWS) uses rolling deployments for updating EC2 instances in Auto Scaling groups. They update 20% of instances at a time while maintaining service availability.

---

### 4. Feature Flags/Toggles

**What it is:** Deploy code with new features disabled. Turn them on via configuration without redeploying.

**How it works:**
```javascript
// Code deployed to production
if (featureFlags.isEnabled('new_checkout_flow')) {
    showNewCheckout(); // Only shown if flag is ON
} else {
    showOldCheckout(); // Default behavior
}
```

**Control:**
- Enable for specific users (user ID, email)
- Enable for specific regions (country, city)
- Enable for percentage of traffic (10%, 50%)
- Enable/disable instantly without code deployment

**Pros:**
- Deploy anytime, release later
- A/B testing capabilities
- Instant kill switch for problematic features
- Progressive rollout control

**Cons:**
- Code becomes complex with many flags
- Technical debt if flags not cleaned up
- Need feature flag management system

**Real-world example:**
> LinkedIn uses feature flags extensively. When they released their new messaging feature, it was deployed to production but enabled only for LinkedIn employees first, then beta users, then gradually to all 800M+ users.

---

### 5. Shadow Deployment

**What it is:** Deploy new version alongside old version. Both process real traffic, but only old version's results are shown to users.

**How it works:**
```
User Request → Load Balancer
                ↓
            ┌───┴───┐
            ↓       ↓
        Old v1.0  New v2.0
            ↓       ↓
     Show to user  Log results
                   (compare)
```

**Pros:**
- Test with real production traffic
- No risk to users
- Identify performance issues before switching

**Cons:**
- Requires 2x infrastructure
- 2x processing cost
- Complex to implement

**Real-world example:**
> Google uses shadow deployments for search algorithm updates. The new algorithm processes real search queries alongside the production algorithm. Engineers compare relevance, speed, and user satisfaction before switching.

---

### 6. Recreate Deployment

**What it is:** Shut down old version completely, then deploy and start new version.

**How it works:**
```
Step 1: Stop all v1.0 servers → Service DOWN
Step 2: Deploy v2.0 to all servers
Step 3: Start all v2.0 servers → Service UP
```

**Pros:**
- Simple to implement
- Clean state transition
- No version conflicts

**Cons:**
- Service downtime
- Not acceptable for production in most cases
- User-facing impact

**When it's used:**
- Scheduled maintenance windows (2 AM - 4 AM)
- Internal tools with small user base
- Breaking changes that require downtime
- Database migrations requiring full shutdown

**Real-world example:**
> Smaller SaaS companies use this for overnight deployments. They notify users: "Maintenance window: 2 AM - 4 AM EST. Service will be unavailable."

---

## Which Companies Use Which Strategy

### Amazon
**Primary Strategies:**
- **Canary Deployments:** For customer-facing services (50% rollout over 6 hours)
- **Feature Flags:** For Prime Video, Alexa features
- **Blue-Green:** For critical payment services
- **Rolling:** For AWS infrastructure updates

**Notable Practice:** Amazon deploys to production every 11.7 seconds on average.

---

### Netflix
**Primary Strategies:**
- **Blue-Green:** For streaming infrastructure
- **Canary:** For content recommendation algorithms
- **Feature Flags:** For UI experiments (A/B testing)
- **Shadow:** For testing new encoding algorithms

**Notable Practice:** Netflix has a "Chaos Monkey" that randomly kills servers to test resilience during deployments.

---

### Google
**Primary Strategies:**
- **Canary:** For Search, Gmail, YouTube (1% → 5% → 25% → 100%)
- **Shadow:** For search algorithm updates
- **Feature Flags:** For workspace features (Docs, Sheets)
- **Rolling:** For Kubernetes cluster updates

**Notable Practice:** Google often runs multiple versions simultaneously for months during gradual rollouts.

---

### Facebook/Meta
**Primary Strategies:**
- **Canary:** For News Feed, Messenger (regional rollouts)
- **Feature Flags:** Extensive use for A/B testing
- **Rolling:** For backend API updates
- **Shadow:** For ML model validation

**Notable Practice:** Facebook uses "Gatekeeper" (their feature flag system) to control billions of feature flag checks per second.

---

### Flipkart
**Primary Strategies:**
- **Canary:** For Big Billion Day sale features
- **Blue-Green:** For payment gateway
- **Feature Flags:** For regional feature launches (Metro+ cities first)
- **Rolling:** For microservices updates

**Notable Practice:** During Big Billion Day, Flipkart deploys in "freeze mode" with only critical hotfixes allowed.

---

### Spotify
**Primary Strategies:**
- **Canary:** For music recommendation engine
- **Feature Flags:** For premium features, podcast features
- **Rolling:** For backend services
- **A/B Testing:** Every new feature tested with multiple user groups

**Notable Practice:** Spotify runs hundreds of A/B tests simultaneously using feature flags.

---

### Uber
**Primary Strategies:**
- **Canary:** For rider/driver matching algorithm (city-by-city)
- **Feature Flags:** For surge pricing, new features
- **Rolling:** For microservices (2000+ services)
- **Blue-Green:** For payment processing

**Notable Practice:** Uber rolls out changes city-by-city to minimize geographic impact.

---

### Airbnb
**Primary Strategies:**
- **Canary:** For search and booking algorithms
- **Feature Flags:** For host/guest features
- **Rolling:** For API updates
- **Shadow:** For pricing algorithm testing

**Notable Practice:** Airbnb uses "Trebuchet" for feature flagging and gradual rollouts.

---

### Microsoft (Azure, Office 365)
**Primary Strategies:**
- **Canary:** For Azure services (ring-based deployment: Internal → Dogfood → Early Adopters → Public)
- **Blue-Green:** For critical infrastructure
- **Feature Flags:** For Office 365 features
- **Rolling:** For data center updates

**Notable Practice:** Microsoft uses "Safe Deployment Practice" with multiple validation rings.

---

### Twitter/X
**Primary Strategies:**
- **Canary:** For timeline algorithm changes
- **Feature Flags:** For new features (Twitter Blue, verification)
- **Rolling:** For API and backend updates
- **A/B Testing:** For engagement features

**Notable Practice:** Twitter tests features with verified users first before public rollout.

---

## Best Practices

### 1. Monitoring & Observability
- Track error rates, latency, CPU, memory
- Set up alerts for anomalies
- Use dashboards to compare old vs new versions

**Tools:** Datadog, New Relic, Prometheus, Grafana

### 2. Automated Rollback
- Define rollback triggers (error rate > 5%, latency > 2s)
- Automate rollback process
- Don't wait for manual intervention

### 3. Progressive Rollout
- Never deploy to 100% immediately
- Start small (1-5%), increase gradually
- Have checkpoints at 5%, 25%, 50%, 100%

### 4. Database Migrations
- Backward compatible changes first
- Deploy code that works with both old and new schema
- Migrate data separately from code deployment

### 5. Testing in Production
- Use real traffic for validation
- Shadow deployments for critical changes
- Synthetic testing alongside real users

### 6. Communication
- Notify teams before major deployments
- Document rollback procedures
- Have incident response plan ready

---

## Comparison Table

| Strategy | Downtime | Rollback Speed | Infrastructure Cost | Complexity | Best For |
|----------|----------|----------------|---------------------|------------|----------|
| **Blue-Green** | None | Instant (seconds) | High (2x) | Medium | Critical services, databases |
| **Canary** | None | Fast (minutes) | Low | High | User-facing features |
| **Rolling** | None | Slow (hours) | None | Low | Microservices, routine updates |
| **Feature Flags** | None | Instant (toggle off) | None | Medium | New features, A/B testing |
| **Shadow** | None | N/A (testing only) | High (2x) | High | Algorithm changes, ML models |
| **Recreate** | Yes | Slow (redeploy) | None | Very Low | Internal tools, maintenance |

---

## Key Takeaways

1. **No single strategy fits all** - Companies combine multiple approaches
2. **Zero downtime is achievable** - With proper planning and tools
3. **Start simple, evolve** - Begin with rolling, add canary/blue-green as you grow
4. **Monitoring is critical** - You can't deploy safely without good observability
5. **Automate everything** - Manual deployments don't scale
