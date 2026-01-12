package io.github.togar2.pvp.enchantment.enchantments;

import io.github.togar2.pvp.enchantment.CombatEnchantment;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.enchant.Enchantment;

/**
 * Wind Burst enchantment - creates a wind explosion on mace smash attack.
 * The explosion launches the attacker upward, allowing for chain attacks.
 * Applied to maces, levels I-III.
 */
public class WindBurstEnchantment extends CombatEnchantment {
	/**
	 * Minimum fall distance required to trigger wind burst (1.5 blocks)
	 */
	public static final float MIN_FALL_DISTANCE = 1.5F;

	/**
	 * Base explosion radius
	 */
	public static final float EXPLOSION_RADIUS = 3.5F;

	public WindBurstEnchantment(EquipmentSlot... slotTypes) {
		super(Enchantment.WIND_BURST, slotTypes);
	}

	/**
	 * Gets the vertical knockback strength for the given level.
	 * Level 1: 1.2
	 * Level 2: 1.75
	 * Level 3: 2.2
	 * Higher levels: 1.5 + 0.35 * level
	 *
	 * @param level the enchantment level
	 * @return the knockback strength
	 */
	public static float getKnockbackStrength(int level) {
		if (level <= 0) return 0.0F;
		return switch (level) {
			case 1 -> 1.2F;
			case 2 -> 1.75F;
			case 3 -> 2.2F;
			default -> 1.5F + 0.35F * level;
		};
	}
}
