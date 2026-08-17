package dev.lifesteal.scoreboard.integration;

/** Lifecycle boundary that keeps PlaceholderAPI classes optional at runtime. */
@FunctionalInterface
public interface PlaceholderApiBridge {

    void unregister();
}
