package io.github.togar2.pvp.enchantment.enchantments;

import io.github.togar2.pvp.enchantment.CombatEnchantment;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.enchant.Enchantment;

/**
 * Lunge enchantment - propels the attacker forward during spear stab.
 * Applied after piercing attack, launches the player in their look direction.
 * Applied to spears, levels I-III.
 */
public class LungeEnchantment extends CombatEnchantment {
	/**
	 * Forward impulse strength per level
	 */
	public static final float IMPULSE_PER_LEVEL = 0.458F;

	/**
	 * Exhaustion cost per level
	 */
	public static final float EXHAUSTION_PER_LEVEL = 4.0F;

	/**
	 * Minimum food level required to lunge (6 food points = 3 drumsticks)
	 */
	public static final int MIN_FOOD_LEVEL = 6;

	public LungeEnchantment(EquipmentSlot... slotTypes) {
		super(Enchantment.LUNGE, slotTypes);
	}

	/**
	 * Gets the forward impulse strength for the given level.
	 *
	 * @param level the enchantment level
	 * @return the impulse strength
	 */
	public static float getImpulseStrength(int level) {
		if (level <= 0) return 0.0F;
		return level * IMPULSE_PER_LEVEL;
	}

	/**
	 * Gets the exhaustion cost for the given level.
	 *
	 * @param level the enchantment level
	 * @return the exhaustion amount
	 */
	public static float getExhaustion(int level) {
		if (level <= 0) return 0.0F;
		return level * EXHAUSTION_PER_LEVEL;
	}
}
