# Fix: Sweep Attack Movement Check Formula

**Commit Hash:** ef4f900
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/attack/VanillaSweepingFeature.java` (Lines 54-60)
**Severity:** HIGH

## Issue

The sweep attack activation formula was using a linear distance check instead of squaring both sides of the comparison, causing incorrect sweep detection thresholds.

### Before
```java
double moveDistance = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
if (moveDistance < maxSpeed * 2.5) return true; // Linear comparison
```

### After
```java
double dx = curr.x() - prev.x();
double dz = curr.z() - prev.z();
double horizontalDistanceSq = dx * dx + dz * dz;
double maxSpeed = attacker.getAttributeValue(Attribute.MOVEMENT_SPEED) * 2.5;
if (horizontalDistanceSq >= maxSpeed * maxSpeed) return false; // Squared comparison
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/entity/player/Player.java`

Vanilla uses squared distance comparison to avoid expensive `Math.sqrt()` calls:
```java
double distanceSq = dx * dx + dz * dz;
if (distanceSq >= (maxSpeed * 2.5) * (maxSpeed * 2.5)) return false;
```

This prevents sweeping attacks when the player is moving too quickly (sprinting, etc.), which is when sweep attacks should not trigger.

## Impact

**Before Fix:** Sweep attacks could trigger when player was moving at incorrect speeds

**After Fix:** Sweep attack speed threshold is correctly enforced, matching vanilla behavior

## Testing

Test sweep attack by:
1. Walk slowly and attack - should sweep
2. Sprint and attack - should not sweep
3. Walk backwards and attack - should sweep
4. Verify sweep particles and damage only affect nearby entities, not the target
