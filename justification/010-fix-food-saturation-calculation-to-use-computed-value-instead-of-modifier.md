# Fix: Food Saturation Calculation to Use Computed Value Instead of Modifier

**Commit Hash:** 2540e74
**File Modified:** `src/main/java/io/github/togar2/pvp/feature/food/VanillaFoodFeature.java` (Line 163)
**Severity:** CRITICAL

## Issue

The `eat(Player, ItemStack)` method was passing the raw `saturationModifier` directly to `addFood()`, but it should compute the saturation value using the formula: `nutrition * saturationModifier * 2.0f`.

### Before
```java
@Override
public void eat(Player player, ItemStack stack) {
    Food foodComponent = stack.get(DataComponents.FOOD);
    if (foodComponent == null) return;
    addFood(player, foodComponent.nutrition(), foodComponent.saturationModifier());
}
```

### After
```java
@Override
public void eat(Player player, ItemStack stack) {
    Food foodComponent = stack.get(DataComponents.FOOD);
    if (foodComponent == null) return;
    addFood(player, foodComponent.nutrition(), (float) foodComponent.nutrition() * foodComponent.saturationModifier() * 2.0f);
}
```

## Vanilla Reference

**Reference File:** `/reference/minecraft/net/minecraft/world/food/FoodData.java`

The `eat()` method in vanilla uses the formula `food * saturationModifier * 2.0f` to compute saturation. This is evident from:

1. The `addFood()` method accepts computed saturation, not raw modifiers
2. The `eat(int, float)` overload (line 156) correctly computes: `(float) food * saturationModifier * 2.0f`
3. Minestom's `Food` component stores saturationModifier as a raw multiplier, not pre-computed

## Impact

Players were receiving incorrect saturation values when eating food items from ItemStack. For example:
- **Expected:** Apple with nutrition=4, modifier=0.3f should add saturation of 4 * 0.3f * 2.0f = 2.4f
- **Actual (Before Fix):** Only 0.3f saturation was added
- **Result:** 8x less saturation restoration than vanilla

## Testing

Food items now restore saturation matching vanilla values. This affects all food consumption from ItemStack (not from potion effects).
