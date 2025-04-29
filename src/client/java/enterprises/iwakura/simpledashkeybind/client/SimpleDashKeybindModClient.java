package enterprises.iwakura.simpledashkeybind.client;

import enterprises.iwakura.simpledashkeybind.Packets;
import enterprises.iwakura.simpledashkeybind.client.commons.BaseDashHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static enterprises.iwakura.simpledashkeybind.SimpleDashKeybindMod.MOD_ID;

@Environment(EnvType.CLIENT)
public class SimpleDashKeybindModClient extends BaseDashHandler implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding keyBinding;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Simply Dash Keybind (client) initializing...");

        this.loadLocalConfig();

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.%s.dash".formatted(MOD_ID),
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.%s.dash".formatted(MOD_ID)
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> this.tickDashHandler());
        ClientPlayNetworking.registerGlobalReceiver(Packets.ConfigSyncPayload.ID, (payload, ctx) -> this.handleConfigSyncPacket(payload));
        ClientPlayNetworking.registerGlobalReceiver(Packets.ShowDashPacket.ID, (payload, ctx) -> this.handleShowDashPacket(payload));
    }

    @Override
    protected KeyBinding getKeyBinding() {
        return keyBinding;
    }

    @Override
    protected ItemStack getFeetItemStack() {
        final var player = MinecraftClient.getInstance().player;
        if (player != null) {
            return player.getEquippedStack(EquipmentSlot.FEET);
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected PlayerInfo getPlayerInfo() {
        final var player = MinecraftClient.getInstance().player;
        if (player != null) {
            return new PlayerInfo(player);
        }
        return null;
    }

    @Override
    protected ItemCooldownManager getItemCooldownManager() {
        final var player = MinecraftClient.getInstance().player;
        if (player != null) {
            return player.getItemCooldownManager();
        }
        return null;
    }

    @Override
    protected void showOverlayMessage(Text text) {
        final var client = MinecraftClient.getInstance();
        if (client != null) {
            client.inGameHud.setOverlayMessage(text, false);
        }
    }

    @Override
    protected void playSound(double x, double y, double z, float volume, float pitch) {
        final var client = MinecraftClient.getInstance();
        if (client != null) {
            final var world = client.world;
            if (world != null) {
                final var player = client.player;
                if (player != null) {
                    world.playSound(
                            player,
                            BlockPos.ofFloored(x, y, z),
                            SoundEvent.of(Identifier.of("minecraft", "entity.llama.spit")),
                            player.getSoundCategory(),
                            volume,
                            pitch
                    );
                }
            }
        }
    }

    @Override
    protected void sendPacket(Packets.DashPacket packet) {
        final var client = MinecraftClient.getInstance();
        if (client != null) {
            ClientPlayNetworking.send(packet);
        }
    }

    @Override
    protected void addParticle(double x, double y, double z, double velX, double velY, double velZ) {
        final var client = MinecraftClient.getInstance();
        if (client != null) {
            final var world = client.world;
            if (world != null) {
                world.addParticleClient(
                        ParticleTypes.CLOUD,
                        x,
                        y,
                        z,
                        velX,
                        velY,
                        velZ
                );
            }
        }
    }

    @Override
    protected void setVelocity(Vec3 vec3) {
        final var client = MinecraftClient.getInstance();
        if (client != null) {
            final var player = client.player;
            if (player != null) {
                player.setVelocity(vec3.getX(), vec3.getY(), vec3.getZ());
            }
        }
    }
}
