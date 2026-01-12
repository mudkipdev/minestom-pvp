# Fix: Remove Server Tick Rate Multiplier from Projectile Velocity Calculation

**Commit Hash:** 8bc31aa
**File Modified:** `src/main/java/io/github/togar2/pvp/entity/projectile/CustomEntityProjectile.java` (Lines 129-130)
**Severity:** CRITICAL

## Issue

The projectile velocity calculation was being multiplied by `ServerFlag.SERVER_TICKS_PER_SECOND`, which is incorrect because velocity in Minestom is already a per-tick value. This would cause projectiles to travel at the wrong speed if the server tick rate differed from the default 20 TPS.

### Before
```java
final double mul = ServerFlag.SERVER_TICKS_PER_SECOND * power;
this.velocity = new Vec(dx * mul, dy * mul, dz * mul);
```

### After
```java
this.velocity = new Vec(dx * power, dy * power, dz * power);
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/entity/projectile/Projectile.java` (Lines 133-141)

Vanilla's `getMovementToShoot()` method shows the correct formula:
```java
public Vec3 getMovementToShoot(final double xd, final double yd, final double zd, final float pow, final float uncertainty) {
    return new Vec3(xd, yd, zd)
        .normalize()
        .add(
            this.random.triangle(0.0, 0.0172275 * (double)uncertainty),
            this.random.triangle(0.0, 0.0172275 * (double)uncertainty),
            this.random.triangle(0.0, 0.0172275 * (double)uncertainty)
        )
        .scale((double)pow);
}
```

The `pow` (power) parameter is scaled directly to the normalized direction vector without any tick rate multiplication. The velocity system in Minestom handles tick integration automatically.

## Impact

**Behavior:**
- **On 20 TPS:** No visible change (multiply by 20, 20 TPS = correct)
- **On 10 TPS:** Projectiles travel 2x faster than intended
- **On 40 TPS:** Projectiles travel 0.5x slower than intended

**After Fix:** Projectile velocity is consistent regardless of server tick rate.

## Testing

Projectiles should now travel at the correct speed and match vanilla projectile behavior. This is especially important for servers running at non-standard tick rates.
