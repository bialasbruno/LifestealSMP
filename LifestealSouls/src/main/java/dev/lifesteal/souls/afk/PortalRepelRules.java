package dev.lifesteal.souls.afk;

/** Calculates a horizontal push perpendicular to the plane of a Nether portal. */
public final class PortalRepelRules {

    private static final double MINIMUM_DIRECTION_COMPONENT = 0.05D;

    private PortalRepelRules() {}

    public static PushDirection awayFromPortal(
            PortalAxis portalAxis,
            double previousX,
            double previousZ,
            double portalCenterX,
            double portalCenterZ,
            double fallbackX,
            double fallbackZ) {
        if (portalAxis == PortalAxis.X) {
            double component = chooseComponent(previousZ - portalCenterZ, fallbackZ);
            return new PushDirection(0.0D, sign(component));
        }

        double component = chooseComponent(previousX - portalCenterX, fallbackX);
        return new PushDirection(sign(component), 0.0D);
    }

    private static double chooseComponent(double primary, double fallback) {
        if (Math.abs(primary) >= MINIMUM_DIRECTION_COMPONENT) {
            return primary;
        }
        if (Math.abs(fallback) >= MINIMUM_DIRECTION_COMPONENT) {
            return fallback;
        }
        return 1.0D;
    }

    private static double sign(double value) {
        return value < 0.0D ? -1.0D : 1.0D;
    }

    public enum PortalAxis {
        X,
        Z
    }

    public record PushDirection(double x, double z) {}
}
