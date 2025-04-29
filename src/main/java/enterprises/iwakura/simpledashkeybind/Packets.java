package enterprises.iwakura.simpledashkeybind;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static enterprises.iwakura.simpledashkeybind.SimpleDashKeybindMod.MOD_ID;

public class Packets {

    public static final Identifier CONFIG_SYNC_PACKET = Identifier.of(MOD_ID, "config_sync");
    public static final Identifier DASH_PACKET = Identifier.of(MOD_ID, "dash_control");
    public static final Identifier SHOW_DASH_PACKET = Identifier.of(MOD_ID, "dash_control");

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShowDashPacket.ID, ShowDashPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DashPacket.ID, DashPacket.CODEC);
    }

    @Data
    public static final class ConfigSyncPayload implements CustomPayload {

        public static final CustomPayload.Id<ConfigSyncPayload> ID = new CustomPayload.Id<>(CONFIG_SYNC_PACKET);

        public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.DOUBLE, ConfigSyncPayload::getDashStrength,
                PacketCodecs.DOUBLE, ConfigSyncPayload::getDashStrengthMultiplier,
                PacketCodecs.LONG, ConfigSyncPayload::getDashCooldownMillis,
                ConfigSyncPayload::new
        );

        private double dashStrength;
        private double dashStrengthMultiplier;
        private long dashCooldownMillis;

        public ConfigSyncPayload(double dashStrength, double dashStrengthMultiplier, long dashCooldownMillis) {
            this.dashStrength = dashStrength;
            this.dashStrengthMultiplier = dashStrengthMultiplier;
            this.dashCooldownMillis = dashCooldownMillis;
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    @Data
    @RequiredArgsConstructor
    public static final class DashPacket implements CustomPayload {

        public static final CustomPayload.Id<DashPacket> ID = new CustomPayload.Id<>(DASH_PACKET);

        public static final PacketCodec<RegistryByteBuf, DashPacket> CODEC = PacketCodec.tuple(
                PacketCodecs.DOUBLE, DashPacket::getPosX,
                PacketCodecs.DOUBLE, DashPacket::getPosY,
                PacketCodecs.DOUBLE, DashPacket::getPosZ,
                PacketCodecs.DOUBLE, DashPacket::getVectorX,
                PacketCodecs.DOUBLE, DashPacket::getVectorY,
                PacketCodecs.DOUBLE, DashPacket::getVectorZ,
                DashPacket::new
        );

        private final double posX, posY, posZ;
        private final double vectorX, vectorY, vectorZ;

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    @Data
    @RequiredArgsConstructor
    public static final class ShowDashPacket implements CustomPayload {

        public static final CustomPayload.Id<ShowDashPacket> ID = new CustomPayload.Id<>(SHOW_DASH_PACKET);

        public static final PacketCodec<RegistryByteBuf, ShowDashPacket> CODEC = PacketCodec.tuple(
                PacketCodecs.DOUBLE, ShowDashPacket::getPosX,
                PacketCodecs.DOUBLE, ShowDashPacket::getPosY,
                PacketCodecs.DOUBLE, ShowDashPacket::getPosZ,
                PacketCodecs.DOUBLE, ShowDashPacket::getVectorX,
                PacketCodecs.DOUBLE, ShowDashPacket::getVectorY,
                PacketCodecs.DOUBLE, ShowDashPacket::getVectorZ,
                ShowDashPacket::new
        );

        private final double posX, posY, posZ;
        private final double vectorX, vectorY, vectorZ;

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
