package io.github.togar2.pvp.feature.spear;

import io.github.togar2.pvp.feature.CombatFeature;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

/**
 * Combat feature which handles spear/kinetic weapon mechanics.
 * Spears are held items that deal damage while being used, based on movement speed.
 */
public interface SpearFeature extends CombatFeature {
	SpearFeature NO_OP = new SpearFeature() {
		@Override
		public void handleSpearTick(Player player, ItemStack stack, long ticksUsed) {
		}

		@Override
		public boolean wasRecentlyStabbed(LivingEntity attacker, int entityId) {
			return false;
		}

		@Override
		public void rememberStabbedEntity(LivingEntity attacker, int entityId) {
		}

		@Override
		public void clearStabbedEntities(LivingEntity attacker) {
		}
	};

	/**
	 * Handles the tick logic for a spear being used.
	 *
	 * @param player the player using the spear
	 * @param stack the spear item stack
	 * @param ticksUsed how many ticks the item has been used
	 */
	void handleSpearTick(Player player, ItemStack stack, long ticksUsed);

	/**
	 * Checks if an entity was recently stabbed by the attacker.
	 *
	 * @param attacker the attacking entity
	 * @param entityId the entity ID of the target
	 * @return true if the entity was recently stabbed
	 */
	boolean wasRecentlyStabbed(LivingEntity attacker, int entityId);

	/**
	 * Remembers that an entity was stabbed by the attacker.
	 *
	 * @param attacker the attacking entity
	 * @param entityId the entity ID of the stabbed target
	 */
	void rememberStabbedEntity(LivingEntity attacker, int entityId);

	/**
	 * Clears all stabbed entity records for the attacker.
	 *
	 * @param attacker the attacking entity
	 */
	void clearStabbedEntities(LivingEntity attacker);
}
