# Fix: Remove Tick Rate Multiplier from Loyalty Acceleration

**Commit Hash:** 23a105c
**File Modified:** `src/main/java/io/github/togar2/pvp/entity/projectile/ThrownTrident.java` (Lines 56-57)
**Severity:** CRITICAL

## Issue

The Loyalty enchantment return acceleration for thrown tridents was being multiplied by `ServerFlag.SERVER_TICKS_PER_SECOND`, causing the trident to return to the player at inconsistent speeds depending on server tick rate.

### Before
```java
setVelocity(velocity.mul(0.95).add(vector.normalize().mul(0.05 * loyalty)
        .mul(ServerFlag.SERVER_TICKS_PER_SECOND)));
```

### After
```java
setVelocity(velocity.mul(0.95).add(vector.normalize().mul(0.05 * loyalty)));
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/entity/projectile/arrow/ThrownTrident.java` (Lines 83-86)

Vanilla applies Loyalty return acceleration directly without tick rate scaling:
```java
this.setNoPhysics(true);
Vec3 vec = currentOwner.getEyePosition().subtract(this.position());
this.setPosRaw(this.getX(), this.getY() + vec.y * 0.015 * (double)loyalty, this.getZ());
double accel = 0.05 * (double)loyalty;
this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add(vec.normalize().scale(accel)));
```

The acceleration (`0.05 * loyalty`) is applied per-tick without server tick rate multiplication.

## Impact

**Before Fix:**
- Tridents return much faster on higher-TPS servers
- On 20 TPS: Returns 20x faster than intended
- On 10 TPS: Returns 10x faster than intended

**After Fix:** Loyalty returns tridents at consistent speed regardless of server tick rate.

## Testing

Throw a trident with Loyalty enchantment and verify it returns to the player at vanilla speed. The return should feel fluid and take approximately the same amount of time regardless of the server's configured tick rate.
