package enterprises.iwakura.simplydashkeybind.client;

import enterprises.iwakura.simplydashkeybind.Enchantments;
import enterprises.iwakura.simplydashkeybind.client.commons.DashConfig;
import enterprises.iwakura.simplydashkeybind.client.commons.DashHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static enterprises.iwakura.simplydashkeybind.SimplyDashKeybindMod.MOD_ID;

@Environment(EnvType.CLIENT)
public class SimplyDashKeybindModClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static DashHandler dashHandler;
    private static KeyBinding keyBinding;
    private static DashConfig dashConfig;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Simply Dash Keybind initializing...");

        dashConfig = DashConfig.load();

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.%s.dash".formatted(MOD_ID),
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.%s.dash".formatted(MOD_ID)
        ));

        dashHandler = new DashHandler(
                this::playerInfoSupplier,
                this::dashStrengthFunction,
                this::dashActionConsumer,
                this::dashEffectConsumer,
                this::dashCooldownConsumer,
                this::dashCooldownElapsedRunnable
        );
        dashHandler.setDashStrength(dashConfig.getDashStrength());
        dashHandler.setDashCooldownMillis(dashConfig.getDashCooldownMillis());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            dashHandler.checkDashCooldown();

            while (keyBinding.wasPressed()) {
                if (getEnchantmentLevel() != 0) {
                    dashHandler.processDash();
                }
            }
        });
    }

    /**
     * Get the player info.
     *
     * @return The player info or null if not found.
     */
    private DashHandler.PlayerInfo playerInfoSupplier() {
        var player = getPlayer();
        if (player == null) {
            return null;
        }
        return new DashHandler.PlayerInfo(
                new DashHandler.Vec3(player.getVelocity().x,
                                     player.getVelocity().y,
                                     player.getVelocity().z
                ),
                player.getYaw(),
                player.getPitch()
        );
    }

    /**
     * Dash strength function.
     *
     * @param dashStrength The dash strength.
     *
     * @return The modified dash strength.
     */
    private Double dashStrengthFunction(Double dashStrength) {
        return dashStrength + getEnchantmentLevel() * dashConfig.getDashStrengthMultiplier();
    }

    /**
     * Dash action consumer.
     *
     * @param dashVector The dash vector.
     */
    private void dashActionConsumer(DashHandler.Vec3 dashVector) {
        var player = getPlayer();
        if (player == null) {
            return;
        }
        player.setVelocity(dashVector.getX(), dashVector.getY(), dashVector.getZ());
    }

    /**
     * Dash effect consumer.
     *
     * @param vec3 The dash effect vector.
     */
    private void dashEffectConsumer(DashHandler.Vec3 vec3) {
        var player = getPlayer();
        if (player == null) {
            return;
        }
        player.playSound(SoundEvent.of(Identifier.of("minecraft", "entity.llama.spit")), 1.0f, 1.0f);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                player.clientWorld.addParticleClient(
                        ParticleTypes.CLOUD,
                        player.getX() + (double) i / 2,
                        player.getY(),
                        player.getZ() + (double) j / 2,
                        vec3.getX(),
                        vec3.getY(),
                        vec3.getZ()
                );
            }
        }
        setBootCooldown();
    }

    /**
     * Dash cooldown consumer.
     *
     * @param cooldownSeconds The cooldown in seconds.
     */
    private void dashCooldownConsumer(String cooldownSeconds) {
        var player = getPlayer();
        if (player == null) {
            return;
        }
        player.sendMessage(Text.translatable("text.simplydashkeybind.dash_cooldown", "§c" + cooldownSeconds), true);
    }

    /**
     * Dash cooldown elapsed runnable.
     */
    private void dashCooldownElapsedRunnable() {
        var player = getPlayer();
        if (player == null) {
            return;
        }
        player.sendMessage(Text.translatable("text.simplydashkeybind.dash_ready"), true);
    }

    /**
     * Get the player entity.
     *
     * @return The player entity or null if not found.
     */
    private ClientPlayerEntity getPlayer() {
        var minecraft = MinecraftClient.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return null;
        }
        return minecraft.player;
    }

    /**
     * Get the enchantment level from the player's boots.
     * @return The enchantment level or 0 if not found.
     */
    private int getEnchantmentLevel() {
        var player = getPlayer();
        if (player == null) {
            return 0;
        }
        var playerFeetItem = player.getEquippedStack(EquipmentSlot.FEET);
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
     * Set the boot cooldown.
     */
    private void setBootCooldown() {
        var player = getPlayer();
        if (player == null) {
            return;
        }
        var playerFeetItem = player.getEquippedStack(EquipmentSlot.FEET);
        if (playerFeetItem.isEmpty()) {
            return;
        }
        player.getItemCooldownManager().set(playerFeetItem, (int) dashConfig.getDashCooldownMillis() / 50);
    }

}
