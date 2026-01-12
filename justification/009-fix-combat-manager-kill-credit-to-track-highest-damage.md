# Fix: Combat Manager Kill Credit to Track Highest Damage

**Commit Hash:** bce2ce6
**File Modified:** `src/main/java/io/github/togar2/pvp/damage/combat/CombatManager.java` (Line 199)
**Severity:** MEDIUM

## Issue

The kill credit tracking for non-player entities was using an incorrect comparison operator (`<=` instead of `>`), causing it to track the first entity that dealt damage instead of the entity that dealt the highest damage.

### Before
```java
} else if (attacker instanceof LivingEntity && (entity == null || entry.damage().getAmount() <= livingDamage)) {
    entity = (LivingEntity) attacker;
    livingDamage = entry.damage().getAmount();
}
```

### After
```java
} else if (attacker instanceof LivingEntity && (entity == null || entry.damage().getAmount() > livingDamage)) {
    entity = (LivingEntity) attacker;
    livingDamage = entry.damage().getAmount();
}
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/damagesource/CombatTracker.java` (Lines 116-136)

Vanilla tracks the highest damage entry:
```java
if (entry.damage() > maxDamage) {
    entity = entry.getAttacker();
    maxDamage = entry.damage();
}
```

## Impact

**Before Fix:**
- If Zombie deals 3 damage, Spider deals 4 damage, Zombie gets kill credit
- First attacker wins instead of highest damage dealer

**After Fix:**
- Entity with highest damage dealt gets kill credit
- Matches vanilla combat tracking behavior
- Death messages correctly attribute kills to the actual highest damage dealer

## Testing

Test kill credit by:
1. Have multiple mobs attack player
2. Mob A deals 3 damage, Mob B deals 5 damage, Mob C deals 2 damage
3. Player dies - Mob B should get kill credit
4. Check death message attributes kill to correct mob
5. Verify this matches vanilla servers
