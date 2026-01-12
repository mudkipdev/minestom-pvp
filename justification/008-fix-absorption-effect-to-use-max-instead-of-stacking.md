# Fix: Absorption Effect to Use Max Instead of Stacking

**Commit Hash:** ecc4244
**File Modified:** `src/main/java/io/github/togar2/pvp/potion/effect/AbsorptionPotionEffect.java` (Line 16)
**Severity:** MEDIUM

## Issue

The Absorption potion effect was stacking additively instead of using the maximum value like vanilla. This allowed infinite absorption stacking through repeated effect applications.

### Before
```java
@Override
public void onApplied(LivingEntity entity, int amplifier, CombatVersion version) {
    if (entity instanceof Player player) {
        float newAbsorption = (float) (4 * (amplifier + 1));
        player.setAdditionalHearts(player.getAdditionalHearts() + newAbsorption);
    }
    super.onApplied(entity, amplifier, version);
}
```

### After
```java
@Override
public void onApplied(LivingEntity entity, int amplifier, CombatVersion version) {
    if (entity instanceof Player player) {
        float newAbsorption = (float) (4 * (amplifier + 1));
        player.setAdditionalHearts(Math.max(player.getAdditionalHearts(), newAbsorption));
    }
    super.onApplied(entity, amplifier, version);
}
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/effect/AbsorptionMobEffect.java` (Lines 22-25)

Vanilla uses maximum value:
```java
public void onEffectStarted(final LivingEntity mob, final int amplifier) {
    super.onEffectStarted(mob, amplifier);
    mob.setAbsorptionAmount(Math.max(mob.getAbsorptionAmount(), (float)(4 * (1 + amplifier))));
}
```

## Impact

**Before Fix:**
- Absorption I applied twice: 4 + 4 = 8 hearts
- Absorption III applied twice: 12 + 12 = 24 hearts (unlimited)
- Result: Infinite health through repeated potion applications

**After Fix:**
- Absorption I applied twice: max(4, 4) = 4 hearts
- Absorption III applied twice: max(12, 12) = 12 hearts
- Result: Only the strongest absorption level is used

## Testing

Test absorption effect:
1. Drink Absorption I potion - gain 4 absorption hearts
2. Drink another Absorption I potion - stay at 4 hearts (not 8)
3. Drink Absorption III potion - gain 12 hearts
4. Drink Absorption I after Absorption III - stay at 12 hearts
5. Verify absorption decays correctly when health is not full
