# Fix: Fall Damage Rounding to Use Floor Instead of Ceil

**Commit Hash:** c8ebe66
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/fall/VanillaFallFeature.java` (Line 164)
**Severity:** MEDIUM

## Issue

The fall damage calculation was using `Math.ceil()` for rounding instead of `Math.floor()`, causing incorrect damage values for falls with fractional distance.

### Before
```java
return (int) Math.ceil((fallDistance - safeFallDistance) * entity.getAttributeValue(Attribute.FALL_DAMAGE_MULTIPLIER));
```

### After
```java
return (int) Math.floor((fallDistance - safeFallDistance) * entity.getAttributeValue(Attribute.FALL_DAMAGE_MULTIPLIER));
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/entity/LivingEntity.java` (Line 1789)

Vanilla uses floor rounding:
```java
return Mth.floor(baseDamage * (double)damageModifier * this.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
```

## Impact

**Before Fix:**
- Fall damage of 2.1 blocks → 3 damage (ceil)
- Fall damage of 2.0 blocks → 2 damage (correct)
- Fall damage of 2.9 blocks → 3 damage (ceil)

**After Fix:**
- Fall damage of 2.1 blocks → 2 damage (floor) ✓
- Fall damage of 2.0 blocks → 2 damage ✓
- Fall damage of 2.9 blocks → 2 damage (floor) ✓

Results in slightly less damage than before, matching vanilla's more lenient rounding.

## Testing

Test fall damage by:
1. Fall 3.5 blocks - should take 1 damage
2. Fall 4.5 blocks - should take 2 damage
3. Fall 5.5 blocks - should take 3 damage
4. Compare with vanilla server to verify exact damage values
