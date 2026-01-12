package io.github.togar2.pvp.enchantment.enchantments;

import io.github.togar2.pvp.enchantment.CombatEnchantment;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.enchant.Enchantment;

/**
 * Density enchantment - increases mace smash attack damage per fallen block.
 * Each level adds 0.5 extra damage per block fallen.
 * Applied to maces, levels I-V.
 */
public class DensityEnchantment extends CombatEnchantment {
	/**
	 * Extra damage per block fallen per level
	 */
	public static final float DAMAGE_PER_BLOCK_PER_LEVEL = 0.5F;

	public DensityEnchantment(EquipmentSlot... slotTypes) {
		super(Enchantment.DENSITY, slotTypes);
	}

	/**
	 * Gets the extra smash damage per block fallen for the given level.
	 *
	 * @param level the enchantment level
	 * @return the extra damage per block fallen
	 */
	public static float getSmashDamagePerFallenBlock(int level) {
		if (level <= 0) return 0.0F;
		return level * DAMAGE_PER_BLOCK_PER_LEVEL;
	}
}
