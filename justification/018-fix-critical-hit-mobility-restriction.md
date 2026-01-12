# Fix: Critical Hit Mobility Restriction Only Checks Blindness

**Commit Hash:** 3f2337c7
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/attack/VanillaCriticalFeature.java`
**Severity:** MEDIUM

## Summary

Fixed the mobility restriction check in critical hit detection to only check for Blindness effect, matching vanilla behavior.

## The Issue

**Line Changed:** 45-47

### Before
```java
boolean mobilityRestricted = attacker.hasEffect(PotionEffect.BLINDNESS)
        || attacker.hasEffect(PotionEffect.SLOW_FALLING)
        || attacker.hasEffect(PotionEffect.LEVITATION);
```

### After
```java
boolean mobilityRestricted = attacker.hasEffect(PotionEffect.BLINDNESS);
```

## Vanilla Reference

**File:** `/reference/minecraft/net/minecraft/world/entity/player/Player.java` (Lines 1991-1993)

```java
public boolean isMobilityRestricted() {
    return this.hasEffect(MobEffects.BLINDNESS);
}
```

This method is called in `canCriticalAttack()` (Line 1047):
```java
private boolean canCriticalAttack(final Entity entity) {
    return this.fallDistance > 0.0
        && !this.onGround()
        && !this.onClimbable()
        && !this.isInWater()
        && !this.isMobilityRestricted()  // Only BLINDNESS
        && !this.isPassenger()
        && entity instanceof LivingEntity
        && !this.isSprinting();
}
```

## Impact

- **SLOW_FALLING effect**: Should NOT prevent critical hits in vanilla
- **LEVITATION effect**: Should NOT prevent critical hits in vanilla
- Only **BLINDNESS** prevents critical hits due to mobility restriction

This means players with slow falling (e.g., Ender Dragon fight) or levitation could not perform critical hits in MinestomPvP, but they can in vanilla.

## Testing Recommendations

1. Apply SLOW_FALLING to a player
2. Jump and attack while falling - should now allow critical hits
3. Apply LEVITATION to a player
4. Attack while elevated - should now allow critical hits
5. Apply BLINDNESS - should still prevent critical hits
