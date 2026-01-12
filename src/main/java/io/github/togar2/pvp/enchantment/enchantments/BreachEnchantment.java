package io.github.togar2.pvp.enchantment.enchantments;

import io.github.togar2.pvp.enchantment.CombatEnchantment;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.enchant.Enchantment;

/**
 * Breach enchantment - reduces enemy armor effectiveness.
 * Each level reduces armor effectiveness by 15%.
 * Applied to maces, levels I-IV.
 */
public class BreachEnchantment extends CombatEnchantment {
	/**
	 * Armor effectiveness reduction per level (15% = 0.15)
	 */
	public static final float ARMOR_REDUCTION_PER_LEVEL = 0.15F;

	public BreachEnchantment(EquipmentSlot... slotTypes) {
		super(Enchantment.BREACH, slotTypes);
	}

	/**
	 * Gets the armor effectiveness multiplier for the given level.
	 * At level 1: 85% effectiveness (0.85)
	 * At level 2: 70% effectiveness (0.70)
	 * At level 3: 55% effectiveness (0.55)
	 * At level 4: 40% effectiveness (0.40)
	 *
	 * @param level the enchantment level
	 * @return the armor effectiveness multiplier (1.0 = full effectiveness)
	 */
	public static float getArmorEffectiveness(int level) {
		if (level <= 0) return 1.0F;
		return Math.max(0.0F, 1.0F - (level * ARMOR_REDUCTION_PER_LEVEL));
	}
}
