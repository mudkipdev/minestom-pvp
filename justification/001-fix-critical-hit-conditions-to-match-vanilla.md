# Fix: Critical Hit Conditions to Match Vanilla

**Commit Hash:** 4ef0827
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/attack/VanillaCriticalFeature.java` (Lines 40-56)
**Severity:** HIGH

## Issue

The critical hit detection was missing several vanilla checks, particularly around water detection, mobility restrictions, and the fall distance requirement.

### Changed Code
The `shouldCrit()` method now includes:
1. **Fall distance check** - Uses `VanillaFallFeature.FALL_DISTANCE` tag instead of just velocity check
2. **Water detection** - Checks if player is in water using `FluidUtil.isTouchingWater()`
3. **Mobility restrictions** - Checks for Blindness, Slow Falling, and Levitation effects
4. **Sprint check (modern only)** - Prevents critical hits while sprinting in 1.20+

```java
double fallDistance = attacker.hasTag(VanillaFallFeature.FALL_DISTANCE)
        ? attacker.getTag(VanillaFallFeature.FALL_DISTANCE) : 0;
boolean inWater = attacker instanceof Player player && FluidUtil.isTouchingWater(player);
boolean mobilityRestricted = attacker.hasEffect(PotionEffect.BLINDNESS)
        || attacker.hasEffect(PotionEffect.SLOW_FALLING)
        || attacker.hasEffect(PotionEffect.LEVITATION);

boolean critical = values.strong() && !playerStateFeature.isClimbing(attacker)
        && fallDistance > 0 && !attacker.isOnGround()
        && !inWater && !mobilityRestricted
        && attacker.getVehicle() == null;
if (version.legacy()) return critical;

return critical && !attacker.isSprinting();
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/entity/player/Player.java`

Critical hit conditions in vanilla require:
- Player must be in the air (not on ground)
- Fall distance must be greater than 0
- Player must not be in water
- Player must not be sprinting (1.9+ only)
- Player must not be affected by restrictive potions (Blindness, Slow Falling, Levitation)
- Player must not be climbing
- Player must not be in a vehicle

## Impact

Previously, critical hits could be triggered in water or while affected by mobility-restricting potions, which is incorrect. Players also couldn't critical hit while sprinting in modern versions (1.20.5+), which is now fixed.

## Testing

Test critical hit detection by:
1. Jumping and attacking in air - should crit
2. Attacking in water - should not crit
3. Attacking with Slow Falling effect - should not crit
4. Attacking while sprinting (modern) - should not crit
5. Attacking while climbing - should not crit
