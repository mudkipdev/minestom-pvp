# Fix: Add Powder Snow to Climbing Detection

**Commit Hash:** bab0ff0f
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/state/VanillaPlayerStateFeature.java`
**Severity:** MEDIUM

## Summary

Added powder snow block to climbing detection, matching vanilla behavior where powder snow is treated as climbable independently from the `minecraft:climbable` block tag.

## The Issue

**Line Changed:** 60

### Before
```java
return tag.contains(key);
```

### After
```java
return tag.contains(key) || block.compare(Block.POWDER_SNOW);
```

## Vanilla Reference

**File:** `/reference/minecraft/net/minecraft/world/entity/Entity.java` (Line 925)

```java
private boolean isStateClimbable(final BlockState state) {
    return state.is(BlockTags.CLIMBABLE) || state.is(Blocks.POWDER_SNOW);
}
```

Vanilla explicitly checks for POWDER_SNOW separately from the climbable tag. This is because powder snow has special climbing behavior (players wearing leather boots can climb it).

## Impact

When standing in powder snow, vanilla considers the player to be "climbing" which affects:

1. **Fall Damage Reset**: Climbing resets fall distance
2. **Critical Hits**: Cannot perform critical hits while climbing
3. **Death Messages**: Shows "fell from climbing" message

Without this fix, players in powder snow would:
- Take fall damage when they shouldn't
- Be able to perform critical hits when they shouldn't
- Get incorrect death messages

## Testing Recommendations

1. Stand in powder snow (with leather boots equipped)
2. Verify player is considered "climbing"
3. Attempt a critical hit while in powder snow - should fail
4. Fall into powder snow from height - should reset fall distance
