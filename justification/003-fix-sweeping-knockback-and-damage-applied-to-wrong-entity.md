# Fix: Sweeping Knockback and Damage Applied to Wrong Entity

**Commit Hash:** 688e8bd
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/attack/VanillaSweepingFeature.java` (Lines 75-100)
**Severity:** HIGH

## Issue

Sweeping attacks were applying knockback to the main target instead of nearby entities, and enchantment damage was being calculated using the target instead of the nearby entity.

### Changed Code
```java
// Knockback applied to nearby entity (line 90)
knockbackFeature.applySweepingKnockback(attacker, living);

// Damage calculation uses the nearby entity (lines 92-93)
float currentDamage = sweepingDamage + enchantmentFeature.getAttackDamage(
        attacker.getItemInMainHand(), EntityGroup.ofEntity(living));
```

Previously:
- `knockbackFeature.applySweepingKnockback(attacker, target)` - Wrong entity
- `enchantmentFeature.getAttackDamage(attacker.getItemInMainHand(), EntityGroup.ofEntity(target))` - Wrong entity

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/entity/player/Player.java`

Vanilla's sweep attack damages all nearby entities (except the main target):
```java
for (Entity nearbyEntity : getNearbyEntities(target, range)) {
    if (nearbyEntity == target || nearbyEntity == attacker) continue;
    // Apply knockback to nearbyEntity
    // Apply damage to nearbyEntity (with enchantments calculated per entity)
}
```

## Impact

**Before Fix:**
- Nearby entities took no knockback
- Enchantment bonuses were calculated incorrectly (using main target's group instead of nearby entity's group)

**After Fix:** Sweep attacks now correctly damage and knock back all nearby entities individually.

## Testing

Test sweeping attacks with multiple entities:
1. Hit one entity with 2+ nearby entities - all should take damage and knockback
2. Verify each entity receives correct enchantment bonuses based on their type (armor, undead, etc.)
3. Verify knockback magnitude is appropriate for sweep attacks
