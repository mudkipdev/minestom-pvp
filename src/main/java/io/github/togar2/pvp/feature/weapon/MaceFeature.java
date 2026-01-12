package io.github.togar2.pvp.feature.weapon;

import io.github.togar2.pvp.feature.CombatFeature;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;

/**
 * Combat feature which handles Mace weapon mechanics including fall-distance based damage
 * and AOE knockback on smash attacks.
 */
public interface MaceFeature extends CombatFeature {
	MaceFeature NO_OP = new MaceFeature() {
		@Override
		public boolean canSmashAttack(LivingEntity attacker) {
			return false;
		}

		@Override
		public float getBonusDamage(LivingEntity attacker) {
			return 0;
		}

		@Override
		public void applySmashAttackEffects(LivingEntity attacker, Entity target) {}

		@Override
		public void postSmashAttack(LivingEntity attacker) {}
	};

	/**
	 * Checks if the attacker can perform a smash attack with the Mace.
	 * Requires fall distance > 1.5 blocks and not elytra flying.
	 *
	 * @param attacker the attacking entity
	 * @return true if a smash attack can be performed
	 */
	boolean canSmashAttack(LivingEntity attacker);

	/**
	 * Gets the bonus damage from fall distance for a Mace attack.
	 *
	 * @param attacker the attacking entity
	 * @return the bonus damage amount
	 */
	float getBonusDamage(LivingEntity attacker);

	/**
	 * Applies the effects of a smash attack, including sounds and AOE knockback.
	 *
	 * @param attacker the attacking entity
	 * @param target the primary target of the attack
	 */
	void applySmashAttackEffects(LivingEntity attacker, Entity target);

	/**
	 * Called after a successful smash attack to reset fall distance
	 * and stop downward movement.
	 *
	 * @param attacker the attacking entity
	 */
	void postSmashAttack(LivingEntity attacker);
}
