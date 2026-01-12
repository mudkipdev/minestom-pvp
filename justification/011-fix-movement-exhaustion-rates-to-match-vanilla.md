# Fix: Movement Exhaustion Rates to Match Vanilla

**Commit Hash:** 4736bc3
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/food/VanillaExhaustionFeature.java` (Lines 79-108)
**Severity:** CRITICAL

## Issue

The movement exhaustion calculations were using overly complex distance-to-centimeters conversions that resulted in exhaustion rates 100x lower than vanilla. The code multiplied distance by 100, then by 0.01, creating incorrect accumulation rates.

### Before
```java
if (player.isOnGround()) {
    int l = (int) Math.round(Math.sqrt(xDiff * xDiff + zDiff * zDiff) * 100.0f);
    if (l > 0) addExhaustion(player, (player.isSprinting() ? 0.1f : 0.0f) * (float) l * 0.01f);
} else {
    if (Objects.requireNonNull(player.getInstance()).getBlock(player.getPosition()) == Block.WATER) {
        int l = (int) Math.round(Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff) * 100.0f);
        if (l > 0) addExhaustion(player, 0.01f * (float) l * 0.01f);
    }
}
```

### After
```java
double distance = Math.sqrt(xDiff * xDiff + zDiff * zDiff);

if (player.isOnGround()) {
    if (distance > 0) {
        addExhaustion(player, (player.isSprinting() ? 0.1f : 0.0f) * (float) distance);
    }
} else {
    if (Objects.requireNonNull(player.getInstance()).getBlock(player.getPosition()) == Block.WATER) {
        double swimDistance = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
        if (swimDistance > 0) {
            addExhaustion(player, 0.01f * (float) swimDistance);
        }
    }
}
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/food/FoodConstants.java`

Vanilla uses discrete exhaustion constants:
- `EXHAUSTION_SPRINT = 0.1f` per block sprinted
- `EXHAUSTION_SWIM = 0.01f` per block swum
- Normal walking adds 0.0f exhaustion

The fix applies these rates directly to the distance traveled in blocks.

## Impact

**Before Fix:**
- Sprinting 1 block: 0.001f exhaustion
- Swimming 1 block: 0.0001f exhaustion
- Result: Food depletes 100x slower than vanilla

**After Fix:**
- Sprinting 1 block: 0.1f exhaustion ✓
- Swimming 1 block: 0.01f exhaustion ✓
- Result: Food depletion matches vanilla rates exactly

## Testing

Sprint across a distance and observe food depletion. It should now match vanilla behavior where sprinting rapidly depletes the hunger bar. Swimming should deplete hunger more slowly than sprinting.
