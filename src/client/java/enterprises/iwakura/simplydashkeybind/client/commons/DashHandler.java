package enterprises.iwakura.simplydashkeybind.client.commons;

import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Main class for Simply Dash Keybind commons.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class DashHandler {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DashHandler.class);

    protected final @NonNull Supplier<PlayerInfo> playerInfoSupplier;
    protected final @NonNull Function<Double, Double> dashStrengthFunction;
    protected final @NonNull Consumer<Vec3> dashActionConsumer;
    protected final @NonNull Consumer<Vec3> dashEffectConsumer;
    protected final @NonNull Consumer<String> dashCooldownConsumer;
    protected final @NonNull Runnable dashCooldownElapsedRunnable;

    protected boolean dashCooldownNotified = false;
    protected long lastDashAtMillis;
    protected double dashStrength;
    protected long dashCooldownMillis;

    /**
     * Checks if the dash cooldown is elapsed and notifies the player.
     */
    public void checkDashCooldown() {
        if (!dashCooldownNotified && canDash()) {
            dashCooldownElapsedRunnable.run();
            dashCooldownNotified = true;
        }
    }

    /**
     * Processes the dash effect.
     */
    public void processDash() {
        if (!canDash()) {
            dashCooldownConsumer.accept(String.format("%.1f", getDashCooldownInSeconds()));
            return;
        }

        PlayerInfo playerInfo = playerInfoSupplier.get();

        if (playerInfo == null) {
            return;
        }

        final var dashVector = calculateDashVector(playerInfo);
        dashActionConsumer.accept(dashVector);

        final var dashEffectVector = calculateDashEffectVector(playerInfo);
        dashEffectConsumer.accept(dashEffectVector);

        lastDashAtMillis = System.currentTimeMillis();
        dashCooldownNotified = false;
    }

    /**
     * Calculates the dash vector based on the player's velocity and direction.
     *
     * @param playerInfo The player info object containing the player's velocity and direction.
     *
     * @return The calculated dash vector.
     */
    public Vec3 calculateDashVector(PlayerInfo playerInfo) {
        double yaw = Math.toRadians(playerInfo.getYaw());
        double pitch = Math.toRadians(playerInfo.getPitch());

        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);

        double velX = playerInfo.getVelocity().getX();
        double velY = playerInfo.getVelocity().getY();
        double velZ = playerInfo.getVelocity().getZ();

        double modifiedDashStrength = dashStrengthFunction.apply(this.dashStrength);

        return new Vec3(
                velX + x * modifiedDashStrength * 1.3,
                velY + y * modifiedDashStrength,
                velZ + z * modifiedDashStrength * 1.3
        );
    }

    public Vec3 calculateDashEffectVector(PlayerInfo playerInfo) {
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
    public double getDashCooldownInSeconds() {
        return (lastDashAtMillis + dashCooldownMillis - System.currentTimeMillis()) / 1000.0;
    }

    /**
     * Checks if the dash is ready to be used.
     *
     * @return True if the dash is ready, false otherwise.
     */
    public boolean canDash() {
        return System.currentTimeMillis() - lastDashAtMillis >= dashCooldownMillis;
    }

    @Data
    @RequiredArgsConstructor
    public static class PlayerInfo {

        private final Vec3 velocity;
        private final double yaw;
        private final double pitch;

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
