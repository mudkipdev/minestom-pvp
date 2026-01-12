# Fix: Shield Damage Formula and Knockback Target

**Commit Hash:** fcf91529
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/block/VanillaBlockFeature.java`
**Severity:** HIGH

## Summary

Fixed two critical issues with shield blocking mechanics:
1. Shield durability damage formula had incorrect offset and threshold
2. Shield knockback was applied to wrong entity (attacker instead of defender)

---

## Issue 1: Shield Damage Formula

**Lines Changed:** 102-103

### Before
```java
if (amount >= 3) {
    int shieldDamage = 1 + (int) Math.floor(amount);
```

### After
```java
if (amount >= 1) {
    int shieldDamage = (int) Math.floor(amount);
```

### Vanilla Reference

**File:** `/reference/minecraft/net/minecraft/world/item/component/BlocksAttacks.java` (Lines 198-201)

```java
public static final BlocksAttacks.ItemDamageFunction DEFAULT =
    new BlocksAttacks.ItemDamageFunction(1.0F, 0.0F, 1.0F);

public int apply(final float dealtDamage) {
    return dealtDamage < this.threshold ? 0 : Mth.floor(this.base + this.factor * dealtDamage);
}
```

Default values: `threshold=1.0F, base=0.0F, factor=1.0F`

Formula: `if (damage < 1.0) return 0; else return floor(0 + 1.0 * damage)`

### Impact

| Damage | MinestomPvP Before | Vanilla/After |
|--------|-------------------|---------------|
| 0.5    | 0                 | 0             |
| 1.0    | 0                 | 1             |
| 2.0    | 0                 | 2             |
| 3.0    | 4                 | 3             |
| 5.0    | 6                 | 5             |
| 10.0   | 11                | 10            |

- **Threshold**: MinestomPvP was `3.0`, vanilla is `1.0`
- **Formula offset**: MinestomPvP added `+1`, vanilla doesn't
- Result: Shields took no damage at low damage values (1-2), then too much damage at higher values

---

## Issue 2: Shield Knockback Target

**Lines Changed:** 133-136

### Before
```java
attacker.takeKnockback(0.5F,
        attackerPos.x() - entityPos.x(),
        attackerPos.z() - entityPos.z()
);
```

### After
```java
entity.takeKnockback(0.5F,
        entityPos.x() - attackerPos.x(),
        entityPos.z() - attackerPos.z()
);
```

### Vanilla Reference

**File:** `/reference/minecraft/net/minecraft/world/entity/LivingEntity.java` (Lines 1363-1368)

```java
protected void blockUsingItem(final ServerLevel level, final LivingEntity attacker) {
    attacker.blockedByItem(this);
}

protected void blockedByItem(final LivingEntity defender) {
    defender.knockback(0.5, defender.getX() - this.getX(), defender.getZ() - this.getZ());
}
```

In vanilla:
- `blockUsingItem` is called on the DEFENDER (entity with shield)
- It calls `attacker.blockedByItem(defender)`
- Inside `blockedByItem`: `this` = attacker, `defender` = shield holder
- Knockback applied to DEFENDER with direction `defender - attacker`

### Impact

**Before (MinestomPvP):**
- Knockback applied to ATTACKER
- Direction: attacker → defender
- Effect: When you hit someone's shield, YOU get pushed back

**After (Vanilla):**
- Knockback applied to DEFENDER (shield holder)
- Direction: defender → attacker
- Effect: When someone hits YOUR shield, YOU get slight recoil toward attacker

This is a fundamental gameplay difference in shield mechanics behavior.

---

## Testing Recommendations

1. **Shield Durability:**
   - Block 1 damage attack - shield should take 1 durability
   - Block 3 damage attack - shield should take 3 durability (not 4)
   - Block 0.5 damage attack - shield should take 0 durability

2. **Shield Knockback:**
   - Block an attack while standing still
   - Verify the DEFENDER (shield holder) gets slight knockback, not the attacker
   - Knockback should be toward the attacker (recoil effect)

3. **Special Mobs:**
   - Test blocking against Hoglin, Ravager, Zoglin
   - These override `blockedByItem` with custom behavior in vanilla
