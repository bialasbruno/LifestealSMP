package dev.lifesteal.souls.afk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalRepelRulesTest {

    @Test
    void xAxisPortalPushesAlongZBackToTheApproachSide() {
        PortalRepelRules.PushDirection south = PortalRepelRules.awayFromPortal(
                PortalRepelRules.PortalAxis.X, 10.5D, 21.0D, 10.5D, 20.5D, 0.0D, 0.0D);
        PortalRepelRules.PushDirection north = PortalRepelRules.awayFromPortal(
                PortalRepelRules.PortalAxis.X, 10.5D, 20.0D, 10.5D, 20.5D, 0.0D, 0.0D);

        assertEquals(new PortalRepelRules.PushDirection(0.0D, 1.0D), south);
        assertEquals(new PortalRepelRules.PushDirection(0.0D, -1.0D), north);
    }

    @Test
    void zAxisPortalPushesAlongXBackToTheApproachSide() {
        PortalRepelRules.PushDirection east = PortalRepelRules.awayFromPortal(
                PortalRepelRules.PortalAxis.Z, 11.0D, 20.5D, 10.5D, 20.5D, 0.0D, 0.0D);
        PortalRepelRules.PushDirection west = PortalRepelRules.awayFromPortal(
                PortalRepelRules.PortalAxis.Z, 10.0D, 20.5D, 10.5D, 20.5D, 0.0D, 0.0D);

        assertEquals(new PortalRepelRules.PushDirection(1.0D, 0.0D), east);
        assertEquals(new PortalRepelRules.PushDirection(-1.0D, 0.0D), west);
    }

    @Test
    void movementFallbackHandlesAPlayerAlreadyInsideThePortalPlane() {
        PortalRepelRules.PushDirection direction = PortalRepelRules.awayFromPortal(
                PortalRepelRules.PortalAxis.X, 10.5D, 20.5D, 10.5D, 20.5D, 0.0D, -0.2D);

        assertEquals(new PortalRepelRules.PushDirection(0.0D, -1.0D), direction);
    }
}
