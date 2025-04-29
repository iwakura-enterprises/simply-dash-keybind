package enterprises.iwakura.simpledashkeybind;

import enterprises.iwakura.simpledashkeybind.commons.DashConfig;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

@Slf4j
public class SimpleDashKeybindMod implements ModInitializer {

    public static final String MOD_ID = "simpledashkeybind";

    private static DashConfig dashConfig;

    @Override
    public void onInitialize() {
        dashConfig = DashConfig.load();

        Enchantments.initialize();
        Packets.initialize();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            log.info("Player {} joined, syncing config...", handler.getPlayer().getName().getString());

            Packets.ConfigSyncPayload payload = new Packets.ConfigSyncPayload(
                    dashConfig.getDashStrength(),
                    dashConfig.getDashStrengthMultiplier(),
                    dashConfig.getDashCooldownMillis()
            );

            sender.sendPacket(payload);
        });

        ServerPlayNetworking.registerGlobalReceiver(Packets.DashPacket.ID, ((payload, ctx) -> {
            final var server = ctx.server();
            final var player = ctx.player();

            log.debug("Received dash packet from player {}: {}", player, payload);

            final var playersInRange = server.getPlayerManager().getPlayerList().stream()
                    .filter(p -> p.squaredDistanceTo(player) < 32)
                    .toList();

            playersInRange.forEach(p -> {
                if (!p.equals(player)) {
                    ServerPlayNetworking.send(p, new Packets.ShowDashPacket(
                            payload.getPosX(), payload.getPosY(), payload.getPosZ(),
                            payload.getVectorX(), payload.getVectorY(), payload.getVectorZ()
                    ));
                }
            });
        }));

        log.info("Simple Dash Keybind initialized with config: {}", dashConfig);
    }
}
