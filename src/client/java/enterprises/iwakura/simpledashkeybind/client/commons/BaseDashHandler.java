package enterprises.iwakura.simpledashkeybind.client.commons;

import enterprises.iwakura.simpledashkeybind.Enchantments;
import enterprises.iwakura.simpledashkeybind.Packets;
import enterprises.iwakura.simpledashkeybind.commons.DashConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

/**
 * Base class for handling dash actions in the game.
 */
@Slf4j
public abstract class BaseDashHandler {

    protected DashConfig useDashConfig;
    protected DashConfig localDashConfig;

    protected boolean dashCooldownNotified = false;
    protected long lastDashAtMillis;

    /**
     * Dash keybinding.
     *
     * @return The keybinding for the dash action.
     */
    protected abstract KeyBinding getKeyBinding();

    /**
     * Get the item stack of the player's feet.
     *
     * @return The item stack of the player's feet.
     */
    protected abstract ItemStack getFeetItemStack();

    /**
     * Get the player info.
     *
     * @return The player info.
     */
    protected abstract PlayerInfo getPlayerInfo();

    /**
     * Get the particle effect center position.
     *
     * @return The particle effect center position.
     */
    protected abstract ItemCooldownManager getItemCooldownManager();

    /**
     * Get the particle effect center position.
     *
     * @param text The text to show.
     */
    protected abstract void showOverlayMessage(Text text);

    /**
     * Play a sound effect.
     *
     * @param x      X Position
     * @param y      Y Position
     * @param z      Z Position
     * @param volume Volume
     * @param pitch  Pitch
     */
    protected abstract void playSound(double x, double y, double z, float volume, float pitch);

    /**
     * Send a packet to the server.
     *
     * @param packet The packet to send.
     */
    protected abstract void sendPacket(Packets.DashPacket packet);

    /**
     * Add a particle effect.
     *
     * @param x    X Position
     * @param y    Y Position
     * @param z    Z Position
     * @param velX X Velocity
     * @param velY Y Velocity
     * @param velZ Z Velocity
     */
    protected abstract void addParticle(double x, double y, double z, double velX, double velY, double velZ);

    /**
     * Set the velocity of the player.
     *
     * @param vec3 The velocity vector to set.
     */
    protected abstract void setVelocity(Vec3 vec3);

    /**
     * Loads the local configuration.
     */
    public void loadLocalConfig() {
        localDashConfig = DashConfig.load();
        useDashConfig = localDashConfig;
    }

    /**
     * Loads and uses the remote configuration.
     *
     * @param dashConfig The remote configuration to load and use.
     */
    public void loadAndUseRemoteConfig(DashConfig dashConfig) {
        useDashConfig = dashConfig;
    }

    /**
     * Unloads the remote configuration and reverts to the local configuration.
     */
    public void unloadRemoteConfig() {
        useDashConfig = localDashConfig;
    }

    /**
     * Ticks the dash handler.
     */
    public void tickDashHandler() {
        checkDashCooldown();

        while (getKeyBinding().wasPressed()) {
            if (getEnchantmentLevel() != 0) {
                processDash();
            } else {
                showOverlayMessage(Text.translatable("text.simpledashkeybind.dash_no_enchantment"));
            }
        }
    }

    /**
     * Checks if the dash cooldown is elapsed and notifies the player.
     */
    protected void checkDashCooldown() {
        if (!dashCooldownNotified && canDash()) {
            showOverlayMessage(Text.translatable("text.simpledashkeybind.dash_ready"));
            dashCooldownNotified = true;
        }
    }

    /**
     * Processes the dash effect.
     */
    protected void processDash() {
        if (canDash()) {
            final var playerInfo = getPlayerInfo();
            if (playerInfo != null) {
                final var dashVector = calculateDashVector(playerInfo);
                setVelocity(dashVector);
                final var dashEffectVector = calculateDashEffectVector(playerInfo);
                createDashEffect(dashEffectVector);
                lastDashAtMillis = System.currentTimeMillis();
                dashCooldownNotified = false;
                sendPacket(new Packets.DashPacket(
                                   dashEffectVector.x,
                                   dashEffectVector.y,
                                   dashEffectVector.z,
                                   playerInfo.getPosition().x,
                                   playerInfo.getPosition().y,
                                   playerInfo.getPosition().z
                           )
                );
            }
        } else {
            showOverlayMessage(Text.translatable("text.simpledashkeybind.dash_cooldown", "§c" + String.format("%.1f", getDashCooldownInSeconds())));
        }
    }

    /**
     * Creates the dash effect.
     *
     * @param dashEffectVector The dash effect vector.
     */
    protected void createDashEffect(Vec3 dashEffectVector) {
        final var playerInfo = getPlayerInfo();
        if (playerInfo != null) {
            final var playerPos = getPlayerInfo().getPosition();
            playSound(playerPos.getX(), playerPos.getY(), playerPos.getZ(), 1.0f, 1.0f);
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    addParticle(
                            playerPos.getX() + (double) i / 2,
                            playerPos.getY(),
                            playerPos.getZ() + (double) j / 2,
                            dashEffectVector.getX(),
                            dashEffectVector.getY(),
                            dashEffectVector.getZ()
                    );
                }
            }
            final var feetItemStack = getFeetItemStack();
            if (!feetItemStack.isEmpty()) {
                getItemCooldownManager().set(feetItemStack, (int) useDashConfig.getDashCooldownMillis() / 50);
            }
        }
    }

    /**
     * Calculates the dash vector based on the player's velocity and direction.
     *
     * @param playerInfo The player info object containing the player's velocity and direction.
     *
     * @return The calculated dash vector.
     */
    protected Vec3 calculateDashVector(PlayerInfo playerInfo) {
        double yaw = Math.toRadians(playerInfo.getYaw());
        double pitch = Math.toRadians(playerInfo.getPitch());

        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);

        double velX = playerInfo.getVelocity().getX();
        double velY = playerInfo.getVelocity().getY();
        double velZ = playerInfo.getVelocity().getZ();

        double modifiedDashStrength = useDashConfig.getDashStrength() + getEnchantmentLevel() * useDashConfig.getDashStrengthMultiplier();

        return new Vec3(
                velX + x * modifiedDashStrength * 1.3,
                velY + y * modifiedDashStrength,
                velZ + z * modifiedDashStrength * 1.3
        );
    }

    /**
     * Calculates the dash effect vector based on the player's velocity and direction.
     *
     * @param playerInfo The player info object containing the player's velocity and direction.
     *
     * @return The calculated dash effect vector.
     */
    protected Vec3 calculateDashEffectVector(PlayerInfo playerInfo) {
        final var dashVector = calculateDashVector(playerInfo);
        return new Vec3(
                dashVector.getX() / 2,
                dashVector.getY() / 2,
                dashVector.getZ() / 2
        );
    }

    /**
     * Calculates the dash cooldown in seconds.
     *
     * @return The dash cooldown in seconds.
     */
    protected double getDashCooldownInSeconds() {
        return (lastDashAtMillis + useDashConfig.getDashCooldownMillis() - System.currentTimeMillis()) / 1000.0;
    }

    /**
     * Checks if the dash is ready to be used.
     *
     * @return True if the dash is ready, false otherwise.
     */
    protected boolean canDash() {
        return System.currentTimeMillis() - lastDashAtMillis >= useDashConfig.getDashCooldownMillis();
    }

    /**
     * Get the enchantment level from the player's boots.
     *
     * @return The enchantment level or 0 if not found.
     */
    protected int getEnchantmentLevel() {
        var playerFeetItem = getFeetItemStack();
        if (playerFeetItem.isEmpty()) {
            return 0;
        }
        var enchantments = playerFeetItem.getEnchantments();
        if (enchantments == null) {
            return 0;
        }
        var enchantmentsKeys = enchantments.getEnchantments();
        if (enchantmentsKeys == null || enchantmentsKeys.isEmpty()) {
            return 0;
        }
        for (RegistryEntry<Enchantment> enchantment : enchantmentsKeys) {
            var optionalEnchantmentKey = enchantment.getKey();
            if (optionalEnchantmentKey.isPresent()) {
                var enchantmentKey = optionalEnchantmentKey.get();
                if (enchantmentKey.getValue().equals(Enchantments.DASH_ENCHANTMENT.getValue())) {
                    return enchantments.getLevel(enchantment);
                }
            }
        }
        return 0;
    }

    /**
     * Handles the config sync packet from the server.
     *
     * @param payload The payload of the packet.
     */
    protected void handleConfigSyncPacket(Packets.ConfigSyncPayload payload) {
        log.info("Received config sync packet from server: {}", payload);
        this.loadAndUseRemoteConfig(new DashConfig(
                payload.getDashStrength(),
                payload.getDashStrengthMultiplier(),
                payload.getDashCooldownMillis()
        ));
    }

    /**
     * Handles the show dash packet from the server.
     *
     * @param payload The payload of the packet.
     */
    protected void handleShowDashPacket(Packets.ShowDashPacket payload) {
        log.debug("Received show dash packet from server: {}", payload);
        this.playSound(payload.getPosX(), payload.getPosY(), payload.getPosZ(), 1.0f, 1.0f);
        this.addParticle(payload.getPosX(), payload.getPosY(), payload.getPosZ(),
                         payload.getVectorX(), payload.getVectorY(), payload.getVectorZ()
        );
    }

    @Data
    @RequiredArgsConstructor
    public static class PlayerInfo {

        private final Vec3 position;
        private final Vec3 velocity;
        private final double yaw;
        private final double pitch;

        public PlayerInfo(Entity entity) {
            this.position = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            this.velocity = new Vec3(entity.getVelocity().getX(), entity.getVelocity().getY(), entity.getVelocity().getZ());
            this.yaw = entity.getYaw();
            this.pitch = entity.getPitch();
        }
    }

    /**
     * Represents a 3D vector.
     */
    @Data
    @RequiredArgsConstructor
    public static class Vec3 {

        private final double x;
        private final double y;
        private final double z;
    }
}
