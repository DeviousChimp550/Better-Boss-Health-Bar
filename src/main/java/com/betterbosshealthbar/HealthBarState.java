package com.betterbosshealthbar;

import lombok.Getter;

/**
 * Tracks the true boss health and the animated values drawn by the overlay.
 * All values are fractions of max health, 0-1. Accessed only from the client thread.
 */
class HealthBarState {
    private static final double SNAP_EPSILON = 0.0005;
    // Largest frame step, so a stall does not make the bar jump
    private static final double MAX_FRAME_SECONDS = 0.1;
    private static final double FADE_SECONDS = 1;

    @Getter
    private boolean active;
    @Getter
    private String name = "";
    @Getter
    private int currentHp;
    @Getter
    private int maxHp;

    /**
     * True health fraction reported by the game.
     */
    @Getter
    private double target;
    /**
     * Animated health fraction.
     */
    @Getter
    private double fill;
    /**
     * Animated damage buffer fraction, always >= fill.
     */
    @Getter
    private double buffer;

    @Getter
    private double visibility;

    private int bufferReleaseTick;
    private long flashStartNanos;
    private long lastFrameNanos;

    void reset() {
        active = false;
        name = "";
        currentHp = 0;
        maxHp = 0;
        flashStartNanos = 0;
        lastFrameNanos = 0;
        visibility = 0;
    }

    /**
     * Feed the latest health values from the game. Called every client tick.
     */
    void update(String name, int current, int max, int tick, long nowNanos, int bufferDelayTicks) {
        double fraction = clamp(current / (double) max);

        if (!active || !this.name.equals(name)) {
            // New boss: start from its current health without animating
            active = true;
            this.name = name;
            target = fill = buffer = fraction;
            flashStartNanos = 0;
            bufferReleaseTick = tick;
        } else if (current != currentHp || max != maxHp) {
            if (fraction < target) {
                flashStartNanos = nowNanos;
                bufferReleaseTick = tick + bufferDelayTicks;
            }
            target = fraction;
        }

        currentHp = current;
        maxHp = max;
    }

    /**
     * Step the animation. Called once per rendered frame.
     */
    void animate(long nowNanos, int tick, int fillSpeed, int bufferSpeed, int bufferDelayTicks) {
        double dt = lastFrameNanos == 0 ? 0 : Math.min((nowNanos - lastFrameNanos) / 1e9, MAX_FRAME_SECONDS);
        lastFrameNanos = nowNanos;

        fill = approach(fill, target, fillSpeed, dt);

        if (bufferDelayTicks <= 0 || buffer < fill) {
            buffer = fill;
        } else if (tick >= bufferReleaseTick) {
            buffer = approach(buffer, fill, bufferSpeed, dt);
        }

        // Handles health bar fading in and out on encounter start / end.
        double step = dt / FADE_SECONDS;
        visibility = active ? Math.min(1, visibility + step) : Math.max(0, visibility - step);
    }

    /**
     * @return flash strength from 1 (just hit) fading to 0
     */
    float flashAlpha(long nowNanos, int durationMs) {
        if (durationMs <= 0 || flashStartNanos == 0) {
            return 0f;
        }

        double t = (nowNanos - flashStartNanos) / 1e6 / durationMs;
        if (t >= 1) {
            flashStartNanos = 0;
            return 0f;
        }
        double remaining = 1 - t;
        return (float) (remaining * remaining);
    }

    /**
     * Exponential ease toward the target. Speed 0 snaps instantly.
     */
    private static double approach(double value, double target, int speed, double dt) {
        if (speed <= 0 || Math.abs(target - value) < SNAP_EPSILON) {
            return target;
        }
        return value + (target - value) * (1 - Math.exp(-speed * dt));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    // Helper function that keeps a buffer persistent for preview purposes.
    void previewBuffer() {
     buffer = 0.6;
    }

    void deactivate()
    {
        active = false;
    }

    boolean isVisible()
    {
        return visibility > 0;
    }
}
