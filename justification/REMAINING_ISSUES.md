# Remaining Inaccuracies Found (Not Yet Fixed)

This document lists inaccuracies identified during the comprehensive audit that require more extensive refactoring and were not fixed in this session due to architectural complexity.

## Potion/Effect System Issues

### 1. Effect Tick Mechanism Using Duration Instead of TickCount
**Severity:** CRITICAL
**File:** `src/main/java/io/github/togar2/pvp/feature/effect/VanillaEffectFeature.java` (Lines 83-94)
**Reference:** `/reference/minecraft/net/minecraft/world/effect/MobEffectInstance.java` (Lines 225-241)

**Issue:** MinestomPvP maintains a custom `DURATION_LEFT` map that tracks duration separately, but vanilla uses `tickCount` (entity's global tick counter for infinite effects, or remaining duration for timed effects). This causes effects to trigger at unpredictable times.

**Impact:** Regeneration, Poison, Wither, and other duration-based effects have incorrect trigger frequencies.

**Recommendation:**
- Implement proper tickCount tracking matching vanilla's MobEffectInstance model
- Effects should trigger based on tick alignment, not countdown duration
- This requires architectural changes to how effect duration is tracked

### 2. No Effect Stacking/Update Logic
**Severity:** HIGH
**File:** `src/main/java/io/github/togar2/pvp/feature/effect/VanillaEffectFeature.java` (Lines 108-131)
**Reference:** `/reference/minecraft/net/minecraft/world/effect/MobEffectInstance.java` (Lines 134-177)

**Issue:** Vanilla implements sophisticated effect stacking through `update()` method:
- Compares amplifiers and upgrades if incoming effect is stronger
- Stores previous effects as "hidden effects" when duration is shorter
- Automatically downgrades to hidden effects when main effect expires

MinestomPvP directly calls `addEffect()` without any stacking logic, causing multiple applications of the same effect to not behave correctly.

**Impact:** Applying Strength II after Strength I will not upgrade; duration management is broken.

**Recommendation:**
- Implement MobEffectInstance.update() logic
- Create hidden effects storage system
- Add amplifier comparison and duration-based upgrade logic

### 3. Instant Effects Applied Repeatedly
**Severity:** HIGH
**File:** `src/main/java/io/github/togar2/pvp/potion/effect/CombatPotionEffect.java` (Lines 96-104)
**Reference:** `/reference/minecraft/net/minecraft/world/effect/HealOrHarmMobEffect.java` (Lines 17-47)

**Issue:** Instant effects (Instant Health, Instant Damage) are implemented in `applyUpdateEffect()`, which causes them to be applied on every tick. Vanilla applies instant effects only once through `applyInstantenouEffect()`.

**Impact:** Instant potions heal/damage repeatedly while the effect is active instead of just once.

**Recommendation:**
- Separate instant effect logic from duration-based effects
- Implement `applyInstantenousEffect()` for one-time application
- Ensure instant effects don't enter the regular tick-based effect loop

### 4. Saturation Effect Applied Every Tick
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/potion/effect/CombatPotionEffect.java` (Line 135)
**Reference:** `/reference/minecraft/net/minecraft/world/effect/SaturationMobEffect.java` (Lines 7-20)

**Issue:** Saturation extends `InstantenousMobEffect` in vanilla, meaning it should only apply once. MinestomPvP treats it as a regular effect and applies it every tick.

**Impact:** Saturation effect applies multiple times when it should only apply once.

**Recommendation:**
- Mark Saturation as an instantaneous effect
- Ensure it's only applied during instantaneous effect application, not in the tick loop

## Food System Issues

### 5. Block Breaking Exhaustion Has Unexplained Legacy Multiplier
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/feature/food/VanillaExhaustionFeature.java` (Line 60)
**Reference:** `/reference/minecraft/net/minecraft/world/food/FoodConstants.java`

**Issue:** The code applies `0.025f` for legacy and `0.005f` for modern, but vanilla has no version-based difference. The 0.025f value doesn't match any vanilla constant.

**Impact:** Legacy version has 5x higher mining exhaustion than vanilla.

**Recommendation:**
- Verify actual 1.8.x vanilla mining exhaustion values
- Remove unexplained multipliers or document their source
- Consider if this is a custom balance change vs. a compatibility issue

### 6. Jump Exhaustion Has Unexplained Legacy Multiplier
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/feature/food/VanillaExhaustionFeature.java` (Lines 89, 91)

**Issue:** Legacy version multiplies jump cost by 4x (0.2f/0.05f → 0.8f/0.2f). This 4x multiplier isn't found in vanilla constants.

**Impact:** Legacy version has 4x higher jump exhaustion cost.

**Recommendation:**
- Research actual 1.8.x vanilla values
- Document the reasoning for legacy multipliers
- Consider whether this is intentional balance vs. incorrect

### 7. Attack Exhaustion Has Unexplained Legacy Multiplier
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/feature/food/VanillaExhaustionFeature.java` (Line 116)

**Issue:** Legacy applies `0.3f`, modern applies `0.1f`. The 3x multiplier isn't found in vanilla constants.

**Impact:** Legacy version has 3x higher attack exhaustion.

**Recommendation:**
- Verify 1.8.x vanilla attack exhaustion constants
- Document or remove unexplained legacy multipliers

## Projectile Mechanics Issues

### 8. Bow Draw Time Formula
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/feature/projectile/VanillaBowFeature.java` (Lines 148-156)
**Reference:** `/reference/minecraft/net/minecraft/world/item/BowItem.java` (Lines 74-82)

**Issue:** MinestomPvP converts ticks to real seconds using `SERVER_TICKS_PER_SECOND`, while vanilla uses fixed division by 20. This causes draw time calculations to differ on servers with non-standard tick rates.

**Impact:** On non-20-TPS servers, bow draw time is calculated differently.

**Recommendation:**
- Use fixed division by 20 like vanilla
- Or ensure ServerFlag matches vanilla assumptions

### 9. Crossbow Charge Duration Missing Enchantment Helper
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/feature/projectile/VanillaCrossbowFeature.java` (Lines 217-220)
**Reference:** `/reference/minecraft/net/minecraft/world/item/CrossbowItem.java` (Lines 245-248)

**Issue:** MinestomPvP hardcodes crossbow charge formula, but vanilla uses `EnchantmentHelper.modifyCrossbowChargingTime()` which may apply additional scaling.

**Impact:** Enchantment effects on crossbow charge time may not be properly applied.

**Recommendation:**
- Use EnchantmentHelper integration for charge time modifications
- Ensure Quick Charge enchantment properly affects charge duration

### 10. Trident Damage Uses Custom Enchantment Handler
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/entity/projectile/ThrownTrident.java` (Line 83)
**Reference:** `/reference/minecraft/net/minecraft/world/entity/projectile/arrow/ThrownTrident.java` (Lines 122-127)

**Issue:** Uses `enchantmentFeature.getAttackDamage()` instead of `EnchantmentHelper.modifyDamage()`. The methods may differ in enchantment calculations.

**Impact:** Trident damage may not be calculated consistently with other weapons.

**Recommendation:**
- Standardize on EnchantmentHelper for all damage calculations
- Ensure enchantments affect trident damage the same way as swords

### 11. Loyalty Level Doesn't Use Enchantment Helper
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/entity/projectile/ThrownTrident.java` (Lines 31-32)

**Issue:** Reads enchantment level directly instead of using `EnchantmentHelper.getTridentReturnToOwnerAcceleration()`.

**Impact:** Loyalty enchantment mechanics may not be properly integrated.

**Recommendation:**
- Use enchantment helper for loyalty retrieval
- Ensure consistency with vanilla's enchantment helper pattern

## Fall Damage Issues

### 12. Missing DamageModifier Parameter
**Severity:** HIGH
**File:** `src/main/java/io/github/togar2/pvp/feature/fall/VanillaFallFeature.java` (Lines 162-164)
**Reference:** `/reference/minecraft/net/minecraft/world/entity/LivingEntity.java` (Lines 1780-1790)

**Issue:** The `getFallDamage()` method doesn't accept a `damageModifier` parameter. Vanilla's `Block.fallOn()` method can specify custom damage multipliers (e.g., hay bales reduce fall damage).

**Impact:** No support for blocks that reduce fall damage (hay bales, snow, etc.).

**Recommendation:**
- Add `damageModifier` parameter to `getFallDamage()` method
- Update `handleFallDamage()` to retrieve and use block-specific modifiers
- Requires integration with block system for fall-on detection

### 13. Missing Epsilon Value in Fall Power Calculation
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/feature/fall/VanillaFallFeature.java` (Line 164)

**Issue:** Vanilla includes epsilon value `1.0E-6` in fall power calculation to handle floating-point precision: `fallDistance + 1.0E-6 - safeFallDistance`.

**Impact:** Edge cases near safe fall distance boundaries may behave differently.

**Recommendation:**
- Add epsilon value to fall power calculation
- Improves floating-point stability at distance boundaries

## Cooldown System Issues

### 14. Attack Cooldown Reset Triggers on Hand Animation
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/feature/cooldown/VanillaAttackCooldownFeature.java` (Lines 43-44)
**Reference:** `/reference/minecraft/net/minecraft/world/entity/player/Player.java` (Lines 1863-1876)

**Issue:** MinestomPvP resets attack cooldown on `PlayerHandAnimationEvent`, while vanilla resets on actual attack execution via `onAttack()`.

**Impact:** Cooldown may reset on missed swings or non-attack hand animations.

**Recommendation:**
- Reset cooldown after damage is confirmed, not on hand animation
- Use EntityAttackEvent instead of PlayerHandAnimationEvent

### 15. Missing Separate Item Swap Ticker
**Severity:** MEDIUM
**File:** `src/main/java/io/github/togar2/pvp/feature/cooldown/VanillaAttackCooldownFeature.java`
**Reference:** `/reference/minecraft/net/minecraft/world/entity/player/Player.java` (Lines 1863-1865)

**Issue:** Vanilla maintains separate `attackStrengthTicker` and `itemSwapTicker`. MinestomPvP uses single `LAST_ATTACKED_TICKS` tag.

**Impact:** Item swapping and attacking have identical cooldown behavior when they should be separate.

**Recommendation:**
- Implement separate tracker for item swap cooldown
- Match vanilla's dual-ticker system

## Healing System Issues

### 16. Missing Peaceful Mode Special Healing
**Severity:** LOW
**File:** `src/main/java/io/github/togar2/pvp/feature/food/VanillaRegenerationFeature.java`
**Reference:** `/reference/minecraft/net/minecraft/server/level/ServerPlayer.java` (Lines 765-782)

**Issue:** Vanilla has special PEACEFUL mode healing: every 20 ticks, unconditional 1.0F heal, saturation +1.0F, and food +1 every 10 ticks.

**Impact:** PEACEFUL mode doesn't have vanilla's fast automatic healing/saturation regeneration.

**Recommendation:**
- Add PEACEFUL difficulty check to VanillaRegenerationFeature
- Implement the 20-tick automatic healing cycle

## Summary Statistics

- **Total Issues Found:** 16 remaining issues
- **Critical Issues:** 4 (Potion ticking, effect stacking, instant effects, fall damageModifier)
- **High Issues:** 2 (Saturation instant, fall damageModifier)
- **Medium Issues:** 9 (Exhaustion multipliers, bow/crossbow, trident, epsilon, cooldown issues)
- **Low Issues:** 1 (Peaceful mode healing)

### Priority for Future Work

1. **Phase 1 (Critical):** Effect tick mechanism and stacking logic (enables all other effect fixes)
2. **Phase 2 (High):** Instant effect separation and fall damage modifier support
3. **Phase 3 (Medium):** Legacy exhaustion multiplier verification, projectile enchantment integration, cooldown system
4. **Phase 4 (Low):** Epsilon values, peaceful mode healing, edge case handling

## Notes

These issues were identified through comprehensive audit using sub-agents analyzing vanilla reference code. They represent real inaccuracies compared to vanilla Minecraft, but fixing them requires either:
- Significant architectural refactoring (effect system rewrite)
- Additional integration work (enchantment helper, block system)
- Verification of legacy mechanics (1.8.x compatibility)

The fixes applied in this session (food saturation, movement exhaustion, projectile velocity) addressed the most critical gameplay-affecting issues that could be fixed without major architectural changes.

NOTE FOR FUTURE AI AGENTS: Never make changes to legacy when you do not have access to legacy 1.8 source code available for reference.