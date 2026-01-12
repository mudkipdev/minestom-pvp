# Fix: Remove Tick Rate Multiplier from Riptide Velocity

**Commit Hash:** 3a0cd2e
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/projectile/VanillaTridentFeature.java` (Lines 137-141)
**Severity:** CRITICAL

## Issue

The Riptide enchantment velocity boost was being multiplied by `ServerFlag.SERVER_TICKS_PER_SECOND`, identical to the general projectile velocity issue. This caused Riptide thrust to scale incorrectly with server tick rate.

### Before
```java
player.setVelocity(player.getVelocity().add(new Vec(
        h * (n / length),
        k * (n / length),
        l * (n / length)
).mul(ServerFlag.SERVER_TICKS_PER_SECOND)));
```

### After
```java
player.setVelocity(player.getVelocity().add(new Vec(
        h * (n / length),
        k * (n / length),
        l * (n / length)
)));
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/item/TridentItem.java` (Lines 91-102)

Vanilla applies Riptide using raw velocity addition without any tick rate multiplication:
```java
if (riptideStrength > 0.0F) {
    float yRot = player.getYRot();
    float xRot = player.getXRot();
    float xd = -Mth.sin((double)(yRot * (float) (Math.PI / 180.0))) * Mth.cos((double)(xRot * (float) (Math.PI / 180.0)));
    float yd = -Mth.sin((double)(xRot * (float) (Math.PI / 180.0)));
    float zd = Mth.cos((double)(yRot * (float) (Math.PI / 180.0))) * Mth.cos((double)(xRot * (float) (Math.PI / 180.0)));
    float dist = Mth.sqrt(xd * xd + yd * yd + zd * zd);
    xd *= riptideStrength / dist;
    yd *= riptideStrength / dist;
    zd *= riptideStrength / dist;
    player.push((double)xd, (double)yd, (double)zd);  // No tick rate multiplication
```

The `player.push()` method applies velocity per-tick without additional scaling.

## Impact

**Before Fix:**
- Riptide acceleration varies with server tick rate
- On 20 TPS: 20x stronger than intended
- On 10 TPS: 10x stronger than intended

**After Fix:** Riptide thrust is consistent and matches vanilla behavior at any tick rate.

## Testing

Using Riptide III in water should propel the player at the correct speed. The acceleration should feel consistent and match vanilla servers.
