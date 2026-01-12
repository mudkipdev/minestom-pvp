# Fix: Knockback Direction Randomization Threshold

**Commit Hash:** 66d8fd2a
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/knockback/VanillaKnockbackFeature.java`
**Severity:** LOW

## Summary

Fixed the knockback direction randomization threshold to match vanilla's epsilon value.

## The Issue

**Line Changed:** 53

### Before
```java
while (dx * dx + dz * dz < 0.0001) {
    dx = random.nextDouble(-1, 1) * 0.01;
    dz = random.nextDouble(-1, 1) * 0.01;
}
```

### After
```java
while (dx * dx + dz * dz < 1.0E-5F) {
    dx = random.nextDouble(-1, 1) * 0.01;
    dz = random.nextDouble(-1, 1) * 0.01;
}
```

## Vanilla Reference

**File:** `/reference/minecraft/net/minecraft/world/entity/LivingEntity.java` (Line 1629)

```java
while (xd * xd + zd * zd < 1.0E-5F) {
    xd = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
    zd = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
}
```

**File:** `/reference/minecraft/net/minecraft/util/Mth.java` (Line 25)

```java
public static final float EPSILON = 1.0E-5F;
```

## Impact

- MinestomPvP threshold was `0.0001` (1e-4)
- Vanilla threshold is `1.0E-5F` (1e-5)
- MinestomPvP was **10x more lenient**, triggering randomization at larger distances
- This means knockback direction randomization kicked in when entities were farther apart than vanilla
- Edge case behavior near zero-distance attacks now matches vanilla

## Testing Recommendations

1. Attack an entity at very close range (same position)
2. Verify knockback direction is randomized appropriately
3. Test edge cases where attacker and target are nearly overlapping
