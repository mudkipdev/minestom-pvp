package io.github.togar2.pvp.feature.weapon;

import io.github.togar2.pvp.feature.FeatureType;
import io.github.togar2.pvp.feature.config.DefinedFeature;
import io.github.togar2.pvp.feature.config.FeatureConfiguration;
import io.github.togar2.pvp.feature.fall.FallFeature;
import io.github.togar2.pvp.player.CombatPlayer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;

/**
 * Vanilla implementation of {@link MaceFeature}
 * <p>
 * Implements the Mace weapon mechanics from Minecraft 1.21+:
 * <ul>
 *   <li>Base damage: 5.0, Attack speed: -3.4</li>
 *   <li>Fall-distance based bonus damage scaling</li>
 *   <li>AOE knockback in 3.5 block radius on smash attack</li>
 *   <li>Fall distance reset after successful smash</li>
 * </ul>
 */
public class VanillaMaceFeature implements MaceFeature {
	public static final DefinedFeature<VanillaMaceFeature> DEFINED = new DefinedFeature<>(
			FeatureType.MACE, VanillaMaceFeature::new,
			FeatureType.FALL
	);

	// Mace constants from vanilla
	public static final float SMASH_ATTACK_FALL_THRESHOLD = 1.5F;
	public static final float SMASH_ATTACK_HEAVY_THRESHOLD = 5.0F;
	public static final float SMASH_ATTACK_KNOCKBACK_RADIUS = 3.5F;
	public static final float SMASH_ATTACK_KNOCKBACK_POWER = 0.7F;

	// Sound event keys for Mace (1.21+)
	private static final Key MACE_SMASH_AIR = Key.key("minecraft:item.mace.smash_air");
	private static final Key MACE_SMASH_GROUND = Key.key("minecraft:item.mace.smash_ground");
	private static final Key MACE_SMASH_GROUND_HEAVY = Key.key("minecraft:item.mace.smash_ground_heavy");

	private final FeatureConfiguration configuration;

	private FallFeature fallFeature;

	public VanillaMaceFeature(FeatureConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void initDependencies() {
		this.fallFeature = configuration.get(FeatureType.FALL);
	}

	@Override
	public boolean canSmashAttack(LivingEntity attacker) {
		double fallDistance = fallFeature.getFallDistance(attacker);
		return fallDistance > SMASH_ATTACK_FALL_THRESHOLD && !attacker.isFlyingWithElytra();
	}

	@Override
	public float getBonusDamage(LivingEntity attacker) {
		if (!canSmashAttack(attacker)) {
			return 0.0F;
		}

		double fallDistance = fallFeature.getFallDistance(attacker);
		double damage;

		// Vanilla damage scaling formula:
		// 1.5-3m fall: 4 * fall_distance
		// 3-8m fall: 12 + 2 * (fall_distance - 3)
		// 8m+ fall: 22 + (fall_distance - 8)
		if (fallDistance <= 3.0) {
			damage = 4.0 * fallDistance;
		} else if (fallDistance <= 8.0) {
			damage = 12.0 + 2.0 * (fallDistance - 3.0);
		} else {
			damage = 22.0 + (fallDistance - 8.0);
		}

		return (float) damage;
	}

	@Override
	public void applySmashAttackEffects(LivingEntity attacker, Entity target) {
		if (!canSmashAttack(attacker)) {
			return;
		}

		double fallDistance = fallFeature.getFallDistance(attacker);

		// Play appropriate sound based on target position
		Sound.Source soundSource = attacker instanceof Player ? Sound.Source.PLAYER : Sound.Source.HOSTILE;

		if (target instanceof LivingEntity livingTarget && livingTarget.isOnGround()) {
			// Target on ground - use ground smash sounds
			fallFeature.setExtraFallParticles(attacker, true);
			Key sound = fallDistance > SMASH_ATTACK_HEAVY_THRESHOLD ? MACE_SMASH_GROUND_HEAVY : MACE_SMASH_GROUND;
			attacker.getViewersAsAudience().playSound(Sound.sound(sound, soundSource, 1.0f, 1.0f), attacker);
			if (attacker instanceof Player player) {
				player.playSound(Sound.sound(sound, soundSource, 1.0f, 1.0f));
			}
		} else {
			// Target in air
			attacker.getViewersAsAudience().playSound(Sound.sound(MACE_SMASH_AIR, soundSource, 1.0f, 1.0f), attacker);
			if (attacker instanceof Player player) {
				player.playSound(Sound.sound(MACE_SMASH_AIR, soundSource, 1.0f, 1.0f));
			}
		}

		// Apply AOE knockback
		applyAoeKnockback(attacker, target);
	}

	/**
	 * Applies AOE knockback to nearby entities when a smash attack hits.
	 *
	 * @param attacker the attacking entity
	 * @param target the primary target
	 */
	private void applyAoeKnockback(LivingEntity attacker, Entity target) {
		if (target.getInstance() == null) return;

		double fallDistance = fallFeature.getFallDistance(attacker);
		double radiusSquared = SMASH_ATTACK_KNOCKBACK_RADIUS * SMASH_ATTACK_KNOCKBACK_RADIUS;

		for (Entity nearbyEntity : target.getInstance().getNearbyEntities(target.getPosition(), SMASH_ATTACK_KNOCKBACK_RADIUS + 1)) {
			if (!(nearbyEntity instanceof LivingEntity nearby)) continue;
			if (!isValidKnockbackTarget(attacker, target, nearby)) continue;

			double distanceSq = target.getPosition().distanceSquared(nearby.getPosition());
			if (distanceSq > radiusSquared) continue;

			Vec direction = nearby.getPosition().sub(target.getPosition()).asVec();
			double knockbackPower = getKnockbackPower(attacker, nearby, direction, fallDistance);

			if (knockbackPower > 0) {
				Vec knockbackVector = direction.normalize().mul(knockbackPower);
				nearby.setVelocity(nearby.getVelocity().add(knockbackVector.x(), SMASH_ATTACK_KNOCKBACK_POWER, knockbackVector.z()));

				// Send velocity update to players
				if (nearby instanceof Player otherPlayer) {
					otherPlayer.sendPacket(new EntityVelocityPacket(otherPlayer.getEntityId(), nearby.getVelocity()));
				}
				if (nearby instanceof CombatPlayer combatPlayer) {
					combatPlayer.sendImmediateVelocityUpdate();
				}
			}
		}
	}

	/**
	 * Checks if an entity is a valid target for AOE knockback.
	 */
	private boolean isValidKnockbackTarget(LivingEntity attacker, Entity primaryTarget, LivingEntity nearby) {
		// Not spectator
		if (nearby instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
			return false;
		}

		// Not the attacker or primary target
		if (nearby == attacker || nearby == primaryTarget) {
			return false;
		}

		// Not a marker armor stand
		if (nearby.getEntityType() == EntityType.ARMOR_STAND) {
			// Armor stands in Minestom don't have marker property accessible easily,
			// so we just skip all armor stands for simplicity
			return false;
		}

		// Not flying in creative
		if (nearby instanceof Player player && player.getGameMode() == GameMode.CREATIVE && player.isFlying()) {
			return false;
		}

		return true;
	}

	/**
	 * Calculates the knockback power for an entity based on distance and fall height.
	 * Formula: (3.5 - distance) * 0.7 * (fallDistance > 5 ? 2 : 1) * (1 - knockbackResistance)
	 */
	private double getKnockbackPower(LivingEntity attacker, LivingEntity nearby, Vec direction, double fallDistance) {
		double distance = direction.length();
		double kbResistance = nearby.getAttributeValue(Attribute.KNOCKBACK_RESISTANCE);
		double heavyMultiplier = fallDistance > SMASH_ATTACK_HEAVY_THRESHOLD ? 2.0 : 1.0;

		return (SMASH_ATTACK_KNOCKBACK_RADIUS - distance)
				* SMASH_ATTACK_KNOCKBACK_POWER
				* heavyMultiplier
				* (1.0 - kbResistance);
	}

	@Override
	public void postSmashAttack(LivingEntity attacker) {
		if (!canSmashAttack(attacker)) {
			return;
		}

		// Stop downward movement
		Vec velocity = attacker.getVelocity();
		attacker.setVelocity(velocity.withY(Math.max(velocity.y(), 0.01 * 20))); // 0.01 blocks/tick * 20 ticks/sec

		// Send velocity update to player
		if (attacker instanceof Player player) {
			player.sendPacket(new EntityVelocityPacket(player.getEntityId(), attacker.getVelocity()));
		}
		if (attacker instanceof CombatPlayer combatPlayer) {
			combatPlayer.sendImmediateVelocityUpdate();
		}

		// Reset fall distance
		fallFeature.resetFallDistance(attacker);
	}
}
