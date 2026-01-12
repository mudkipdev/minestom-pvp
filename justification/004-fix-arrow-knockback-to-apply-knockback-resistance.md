# Fix: Arrow Knockback to Apply Knockback Resistance

**Commit Hash:** 0204157
**File Modified:** `src/main/java/io/github/togar2/pvp/entity/projectile/AbstractArrow.java` (Lines 184-195)
**Severity:** HIGH

## Issue

Arrow knockback was not accounting for the target's knockback resistance attribute, and the vertical knockback component was being mishandled.

### Before
```java
if (knockback > 0) {
    Vec knockbackVec = getVelocity()
            .mul(1, 0, 1)
            .normalize().mul(knockback * 0.6);
    knockbackVec = knockbackVec.add(0, 0.1, 0)
            .mul(ServerFlag.SERVER_TICKS_PER_SECOND / 2.0);

    if (knockbackVec.lengthSquared() > 0) {
        Vec newVel = living.getVelocity().add(knockbackVec);
        living.setVelocity(newVel);
    }
}
```

### After
```java
if (knockback > 0) {
    double resistance = Math.max(0.0, 1.0 - living.getAttributeValue(Attribute.KNOCKBACK_RESISTANCE));
    Vec knockbackVec = getVelocity()
            .mul(1, 0, 1)
            .normalize().mul(knockback * 0.6 * resistance);

    if (knockbackVec.lengthSquared() > 0) {
        living.setVelocity(living.getVelocity().add(
                knockbackVec.x() * ServerFlag.SERVER_TICKS_PER_SECOND,
                0.1 * ServerFlag.SERVER_TICKS_PER_SECOND,
                knockbackVec.z() * ServerFlag.SERVER_TICKS_PER_SECOND
        ));
    }
}
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/entity/projectile/arrow/AbstractArrow.java` (Lines 208-220)

Vanilla applies knockback resistance multiplication:
```java
double knockbackResistance = entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
double finalKnockback = knockback * 0.6 * (1.0 - knockbackResistance);
```

## Impact

**Before Fix:**
- Players/mobs with knockback resistance attribute were still knocked back at full force
- Vertical knockback was incorrectly scaled

**After Fix:**
- Knockback resistance now properly reduces arrow knockback
- Vertical and horizontal knockback are correctly applied per-tick
- Entities with protection gear or status effects have appropriate knockback reduction

## Testing

Test arrow knockback:
1. Shoot arrow at unarmored player - should knockback normally
2. Shoot arrow at fully armored player - should knockback less
3. Shoot arrow at entity with knockback resistance - should minimize knockback
4. Verify vertical knockback component sends target up slightly
