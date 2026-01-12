package io.github.togar2.pvp.feature.spear;

import io.github.togar2.pvp.feature.FeatureType;
import io.github.togar2.pvp.feature.RegistrableFeature;
import io.github.togar2.pvp.feature.config.DefinedFeature;
import io.github.togar2.pvp.feature.config.FeatureConfiguration;
import io.github.togar2.pvp.feature.item.ItemDamageFeature;
import io.github.togar2.pvp.feature.knockback.KnockbackFeature;
import io.github.togar2.pvp.player.CombatPlayer;
import io.github.togar2.pvp.utils.ViewUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.ServerFlag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.PlayerBeginItemUseEvent;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import net.minestom.server.event.item.PlayerFinishItemUseEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.item.ItemAnimation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.KineticWeapon;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vanilla implementation of {@link SpearFeature}.
 * <p>
 * Handles spear/kinetic weapon mechanics including:
 * - Contact cooldown tracking for stabbed entities
 * - Speed-based damage calculations
 * - Dismount, knockback, and damage conditions
 * - Forward movement bonus on attack
 */
public class VanillaSpearFeature implements SpearFeature, RegistrableFeature {
	public static final DefinedFeature<VanillaSpearFeature> DEFINED = new DefinedFeature<>(
			FeatureType.SPEAR, VanillaSpearFeature::new,
			VanillaSpearFeature::playerInit,
			FeatureType.ITEM_DAMAGE, FeatureType.KNOCKBACK
	);

	/**
	 * Default contact cooldown in ticks (10 ticks = 0.5 seconds)
	 */
	public static final int DEFAULT_CONTACT_COOLDOWN = 10;

	/**
	 * Tag to store the tick when each entity was stabbed.
	 * Maps entity ID to the tick when they were stabbed.
	 */
	private static final Tag<Map<Integer, Long>> STABBED_ENTITIES = Tag.Transient("stabbedEntities");

	/**
	 * Set of all spear material keys.
	 */
	public static final Set<Key> SPEAR_MATERIAL_KEYS = Set.of(
			Key.key("minecraft:wooden_spear"),
			Key.key("minecraft:stone_spear"),
			Key.key("minecraft:copper_spear"),
			Key.key("minecraft:iron_spear"),
			Key.key("minecraft:golden_spear"),
			Key.key("minecraft:diamond_spear"),
			Key.key("minecraft:netherite_spear")
	);

	private final FeatureConfiguration configuration;

	private ItemDamageFeature itemDamageFeature;
	private KnockbackFeature knockbackFeature;

	public VanillaSpearFeature(FeatureConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void initDependencies() {
		this.itemDamageFeature = configuration.get(FeatureType.ITEM_DAMAGE);
		this.knockbackFeature = configuration.get(FeatureType.KNOCKBACK);
	}

	private static void playerInit(Player player, boolean firstInit) {
		player.setTag(STABBED_ENTITIES, new ConcurrentHashMap<>());
	}

	@Override
	public void init(EventNode<EntityInstanceEvent> node) {
		// Handle spear use start - play sound
		node.addListener(PlayerBeginItemUseEvent.class, event -> {
			if (event.getAnimation() != ItemAnimation.SPEAR) return;

			Player player = event.getPlayer();
			ItemStack stack = event.getItemStack();
			KineticWeapon kineticWeapon = stack.get(DataComponents.KINETIC_WEAPON);
			if (kineticWeapon == null) return;

			// Play the spear use sound
			SoundEvent sound = kineticWeapon.sound();
			if (sound != null) {
				ViewUtil.viewersAndSelf(player).playSound(Sound.sound(
						sound, Sound.Source.PLAYER,
						1.0f, 1.0f
				), player);
			}
		});

		// Handle spear tick - deal damage to entities in range
		node.addListener(PlayerTickEvent.class, event -> {
			Player player = event.getPlayer();
			if (!player.isUsingItem()) return;

			ItemStack stack = player.getItemInHand(player.getPlayerMeta().getActiveHand());
			if (!isSpear(stack)) return;

			KineticWeapon kineticWeapon = stack.get(DataComponents.KINETIC_WEAPON);
			if (kineticWeapon == null) return;

			long ticksUsed = player.getCurrentItemUseTime();
			handleSpearTick(player, stack, ticksUsed);
		});

		// Handle spear use end - clear stabbed entities and damage the item
		node.addListener(PlayerCancelItemUseEvent.class, event -> {
			Player player = event.getPlayer();
			ItemStack stack = event.getItemStack();
			if (!isSpear(stack)) return;

			clearStabbedEntities(player);

			// Damage the spear after use
			if (player.getCurrentItemUseTime() > 0) {
				itemDamageFeature.damageEquipment(player,
						event.getHand() == PlayerHand.MAIN ? EquipmentSlot.MAIN_HAND : EquipmentSlot.OFF_HAND, 1);
			}
		});

		// Handle spear finish (if use duration expires)
		node.addListener(PlayerFinishItemUseEvent.class, event -> {
			Player player = event.getPlayer();
			ItemStack stack = event.getItemStack();
			if (!isSpear(stack)) return;

			clearStabbedEntities(player);
		});
	}

	@Override
	public void handleSpearTick(Player player, ItemStack stack, long ticksUsed) {
		KineticWeapon kineticWeapon = stack.get(DataComponents.KINETIC_WEAPON);
		if (kineticWeapon == null) return;

		int delayTicks = kineticWeapon.delayTicks();
		if (ticksUsed < delayTicks) return;

		int ticksAfterDelay = (int) (ticksUsed - delayTicks);

		// Get attacker's look direction
		Vec lookDirection = getLookDirection(player);

		// Get attacker's motion (velocity scaled to blocks per second, then back to per-tick for comparison)
		Vec attackerMotion = getMotion(player);

		// Calculate attacker's speed projected onto look direction
		double attackerSpeedProjection = lookDirection.dot(attackerMotion);

		// Action factor: 1.0 for players, 0.2 for non-players
		float actionFactor = 1.0f;

		// Get base attack damage
		double baseDamage = player.getAttributeValue(Attribute.ATTACK_DAMAGE);

		// Get attack range for entity detection
		double attackRange = player.getAttributeValue(Attribute.ENTITY_INTERACTION_RANGE);

		// Find entities in range and attack them
		boolean anyAffected = false;
		if (player.getInstance() != null) {
			List<Entity> nearbyEntities = new ArrayList<>();
			player.getInstance().getEntityTracker().nearbyEntities(
					player.getPosition(), attackRange + 1,
					EntityTracker.Target.ENTITIES,
					nearbyEntities::add
			);

			for (Entity entity : nearbyEntities) {
				if (entity == player) continue;
				if (!(entity instanceof LivingEntity living)) continue;
				if (entity.isRemoved() || living.isDead()) continue;

				// Check if entity is within attack range
				if (!isEntityInRange(player, entity, attackRange)) continue;

				// Check contact cooldown
				int contactCooldown = kineticWeapon.contactCooldownTicks();
				if (wasRecentlyStabbed(player, entity.getEntityId())) continue;

				// Remember this entity as stabbed
				rememberStabbedEntity(player, entity.getEntityId());

				// Calculate target's speed projected onto attacker's look direction
				Vec targetMotion = getMotion(entity);
				double targetSpeedProjection = lookDirection.dot(targetMotion);

				// Calculate relative speed (attacker - target, clamped to >= 0)
				double relativeSpeed = Math.max(0.0, attackerSpeedProjection - targetSpeedProjection);

				// Check conditions
				KineticWeapon.Condition dismountCondition = kineticWeapon.dismountConditions();
				KineticWeapon.Condition knockbackCondition = kineticWeapon.knockbackConditions();
				KineticWeapon.Condition damageCondition = kineticWeapon.damageConditions();

				boolean dealsDismount = dismountCondition != null &&
						testCondition(dismountCondition, ticksAfterDelay, attackerSpeedProjection, relativeSpeed, actionFactor);
				boolean dealsKnockback = knockbackCondition != null &&
						testCondition(knockbackCondition, ticksAfterDelay, attackerSpeedProjection, relativeSpeed, actionFactor);
				boolean dealsDamage = damageCondition != null &&
						testCondition(damageCondition, ticksAfterDelay, attackerSpeedProjection, relativeSpeed, actionFactor);

				if (!dealsDismount && !dealsKnockback && !dealsDamage) continue;

				// Calculate damage: base_damage + floor(relative_speed * damage_multiplier)
				float damageMultiplier = kineticWeapon.damageMultiplier();
				float damageDealt = (float) baseDamage + (float) Math.floor(relativeSpeed * damageMultiplier);

				// Perform the stab attack
				boolean affected = performStabAttack(player, living, damageDealt, dealsDamage, dealsKnockback, dealsDismount);
				if (affected) {
					anyAffected = true;

					// Play hit sound
					SoundEvent hitSound = kineticWeapon.hitSound();
					if (hitSound != null) {
						ViewUtil.viewersAndSelf(player).playSound(Sound.sound(
								hitSound, Sound.Source.PLAYER,
								1.0f, 1.0f
						), player);
					}
				}
			}
		}

		// Apply forward movement bonus when attacking
		if (anyAffected) {
			float forwardMovement = kineticWeapon.forwardMovement();
			if (forwardMovement > 0) {
				applyForwardMovement(player, lookDirection, forwardMovement);
			}

			// Trigger hit effect (entity status byte 2 for hit animation)
			player.sendPacketToViewersAndSelf(
					new net.minestom.server.network.packet.server.play.EntityAnimationPacket(
							player.getEntityId(),
							net.minestom.server.network.packet.server.play.EntityAnimationPacket.Animation.SWING_MAIN_ARM
					)
			);
		}
	}

	/**
	 * Tests if a kinetic weapon condition is met.
	 */
	private boolean testCondition(KineticWeapon.Condition condition, int ticksUsed,
								  double attackerSpeed, double relativeSpeed, float entityFactor) {
		return ticksUsed <= condition.maxDurationTicks()
				&& attackerSpeed >= condition.minSpeed() * entityFactor
				&& relativeSpeed >= condition.minRelativeSpeed() * entityFactor;
	}

	/**
	 * Performs a stab attack on the target entity.
	 *
	 * @param attacker the attacking player
	 * @param target the target entity
	 * @param damage the damage to deal
	 * @param dealsDamage whether to deal damage
	 * @param dealsKnockback whether to apply knockback
	 * @param dealsDismount whether to dismount the target
	 * @return true if the attack was successful
	 */
	private boolean performStabAttack(Player attacker, LivingEntity target, float damage,
									  boolean dealsDamage, boolean dealsKnockback, boolean dealsDismount) {
		boolean affected = false;

		// Dismount
		if (dealsDismount && target.getVehicle() != null) {
			target.getVehicle().removePassenger(target);
			affected = true;
		}

		// Apply damage
		if (dealsDamage && damage > 0) {
			boolean damageSucceeded = target.damage(new Damage(
					DamageType.PLAYER_ATTACK,
					attacker, attacker,
					null, damage
			));
			if (damageSucceeded) {
				affected = true;

				// Apply knockback if conditions met
				if (dealsKnockback) {
					knockbackFeature.applyAttackKnockback(attacker, target, 1);
				}
			}
		} else if (dealsKnockback && !dealsDamage) {
			// Apply knockback without damage
			knockbackFeature.applyAttackKnockback(attacker, target, 1);
			affected = true;
		}

		// Send velocity update if affected
		if (affected && target instanceof CombatPlayer combatPlayer) {
			combatPlayer.sendImmediateVelocityUpdate();
		}

		return affected;
	}

	/**
	 * Gets the look direction vector for an entity.
	 */
	private Vec getLookDirection(Entity entity) {
		Pos pos = entity.getPosition();
		float yaw = pos.yaw();
		float pitch = pos.pitch();

		double x = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
		double y = -Math.sin(Math.toRadians(pitch));
		double z = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));

		return new Vec(x, y, z);
	}

	/**
	 * Gets the motion vector for an entity, scaled to blocks per second.
	 * For non-player passengers, uses the root vehicle's motion.
	 */
	private Vec getMotion(Entity entity) {
		Entity movingEntity = entity;
		if (!(entity instanceof Player) && entity.getVehicle() != null) {
			// Get root vehicle
			Entity vehicle = entity.getVehicle();
			while (vehicle.getVehicle() != null) {
				vehicle = vehicle.getVehicle();
			}
			movingEntity = vehicle;
		}

		// Velocity in Minestom is in blocks/second * ticks per second
		// Scale it to blocks per second for comparison
		return movingEntity.getVelocity().div(ServerFlag.SERVER_TICKS_PER_SECOND);
	}

	/**
	 * Checks if an entity is within the attack range of the attacker.
	 */
	private boolean isEntityInRange(Player attacker, Entity target, double range) {
		// Simple distance check for now
		Vec attackerEyePos = attacker.getPosition().asVec().add(0, attacker.getEyeHeight(), 0);
		Vec targetCenter = target.getPosition().asVec().add(0, target.getBoundingBox().height() / 2, 0);

		double distanceSquared = attackerEyePos.distanceSquared(targetCenter);
		return distanceSquared <= range * range;
	}

	/**
	 * Applies forward movement bonus to the attacker.
	 */
	private void applyForwardMovement(Player player, Vec lookDirection, float amount) {
		Vec movement = lookDirection.mul(amount * ServerFlag.SERVER_TICKS_PER_SECOND);
		player.setVelocity(player.getVelocity().add(movement));
	}

	@Override
	public boolean wasRecentlyStabbed(LivingEntity attacker, int entityId) {
		Map<Integer, Long> stabbedMap = getStabbedEntitiesMap(attacker);
		if (stabbedMap == null) return false;

		Long stabbedTick = stabbedMap.get(entityId);
		if (stabbedTick == null) return false;

		// Get the kinetic weapon from the attacker's held item to check cooldown
		ItemStack heldItem = attacker.getItemInMainHand();
		KineticWeapon kineticWeapon = heldItem.get(DataComponents.KINETIC_WEAPON);
		int contactCooldown = kineticWeapon != null ? kineticWeapon.contactCooldownTicks() : DEFAULT_CONTACT_COOLDOWN;

		long currentTick = attacker.getAliveTicks();
		return currentTick - stabbedTick < contactCooldown;
	}

	@Override
	public void rememberStabbedEntity(LivingEntity attacker, int entityId) {
		Map<Integer, Long> stabbedMap = getStabbedEntitiesMap(attacker);
		if (stabbedMap == null) return;

		stabbedMap.put(entityId, attacker.getAliveTicks());
	}

	@Override
	public void clearStabbedEntities(LivingEntity attacker) {
		Map<Integer, Long> stabbedMap = getStabbedEntitiesMap(attacker);
		if (stabbedMap != null) {
			stabbedMap.clear();
		}
	}

	private Map<Integer, Long> getStabbedEntitiesMap(LivingEntity entity) {
		if (!entity.hasTag(STABBED_ENTITIES)) {
			entity.setTag(STABBED_ENTITIES, new ConcurrentHashMap<>());
		}
		return entity.getTag(STABBED_ENTITIES);
	}

	/**
	 * Checks if the given material is a spear.
	 *
	 * @param material the material to check
	 * @return true if it's a spear material
	 */
	public static boolean isSpear(Material material) {
		return SPEAR_MATERIALS.contains(material);
	}
}
