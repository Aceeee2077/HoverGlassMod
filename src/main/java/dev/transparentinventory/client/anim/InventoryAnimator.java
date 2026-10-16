package dev.transparentinventory.client.anim;

import dev.transparentinventory.client.InventoryConfig;

/**
 * Frame-rate independent physics for the inventory panel's horizontal slide.
 *
 * <p>The animator integrates with a fixed maximum sub-step ({@link #MAX_STEP}) so
 * that a 30 FPS client and a 240 FPS client produce the same trajectory for the
 * same wall-clock time. Time is supplied by the caller (derived from
 * {@code System.nanoTime()}), never read from Minecraft's tick delta.
 *
 * <p>State machine: {@code IDLE -> SLIDING -> SETTLING -> STABLE -> CLOSING -> DONE}.
 * Opening slides in from the left with a hard-brake impact, a backwards bounce and
 * a damped-spring settle; closing accelerates left off the screen.
 */
public final class InventoryAnimator {

    public enum Phase {
        IDLE,
        SLIDING,
        SETTLING,
        STABLE,
        CLOSING,
        DONE
    }

    /** Largest delta time accepted from the caller (spec section 24: clamp to 1/20 s). */
    private static final double MAX_DT = 1.0 / 20.0;
    /** Largest integration step, keeps the semi-implicit Euler error frame-rate independent. */
    private static final double MAX_STEP = 1.0 / 240.0;
    /** Settlement is reached when the panel is this close to its target (pixels). */
    private static final double REST_POSITION = 1.0;
    /** Settlement is reached when the velocity is this small (pixels / second). */
    private static final double REST_VELOCITY = 30.0;
    /** Decay speed of the impact flash. */
    private static final double FLASH_DECAY = 9.0;

    private final InventoryConfig config;

    /** Position along the slide axis (the panel's animated left edge). */
    private double pos;
    private double velocity;
    private double targetPos;
    /** Panel extent along the slide axis, used to place it fully off screen. */
    private double extent;
    private Phase phase = Phase.IDLE;
    private float impactFlash;
    private boolean started;

    public InventoryAnimator(InventoryConfig config) {
        this.config = (config == null) ? new InventoryConfig() : config;
    }

    /**
     * Starts the slide-in from the left towards {@code targetPos}.
     *
     * @param targetPos   resting left edge of the panel, in GUI pixels
     * @param startOffset distance behind the target where the panel starts
     *                    (larger than {@code targetPos + extent} to begin fully off screen)
     * @param extent      panel size along the slide axis, used for the off-screen test
     */
    public void beginOpen(double targetPos, double startOffset, double extent) {
        this.targetPos = targetPos;
        this.extent = Math.max(0.0, extent);
        this.pos = targetPos - Math.max(0.0, startOffset);
        this.velocity = Math.abs(config.slideSpeed);
        this.impactFlash = 0.0F;
        this.phase = Phase.SLIDING;
        this.started = true;
    }

    /**
     * Moves the resting position without restarting the animation. Used when the
     * GUI is resized while the panel is already visible.
     */
    public void retarget(double newTargetPos) {
        this.targetPos = newTargetPos;
        if (phase == Phase.STABLE || phase == Phase.IDLE) {
            this.pos = newTargetPos;
            this.velocity = 0.0;
            if (phase == Phase.IDLE) {
                this.phase = Phase.STABLE;
            }
        }
    }

    /** Requests the left-off-screen close animation. Idempotent. */
    public void requestClose() {
        if (phase == Phase.CLOSING || phase == Phase.DONE) {
            return;
        }
        this.velocity = -Math.abs(config.closeSpeed);
        this.phase = Phase.CLOSING;
    }

    /**
     * Advances the simulation by {@code dt} seconds.
     *
     * @param dt wall-clock seconds since the previous update; non-positive values are ignored
     */
    public void update(double dt) {
        if (dt <= 0.0 || phase == Phase.DONE) {
            return;
        }
        double remaining = Math.min(dt, MAX_DT);
        while (remaining > 1.0E-9) {
            double step = Math.min(remaining, MAX_STEP);
            integrate(step);
            remaining -= step;
            if (phase == Phase.DONE) {
                break;
            }
        }
        impactFlash *= (float) Math.exp(-dt * FLASH_DECAY);
        if (impactFlash < 1.0E-3F) {
            impactFlash = 0.0F;
        }
    }

    private void integrate(double step) {
        switch (phase) {
            case SLIDING -> {
                // Hard brake: strong deceleration, never reversing before impact.
                velocity -= config.slideBrake * step;
                if (velocity < 0.0) {
                    velocity = 0.0;
                }
                pos += velocity * step;
                if (pos >= targetPos) {
                    pos = targetPos;
                    velocity = -Math.abs(velocity) * config.bounce;
                    impactFlash = 1.0F;
                    phase = Phase.SETTLING;
                } else if (velocity == 0.0) {
                    // Braked to a halt short of the target: let the spring finish.
                    phase = Phase.SETTLING;
                }
            }
            case SETTLING -> {
                double accel = -config.spring * (pos - targetPos) - config.damping * velocity;
                velocity += accel * step;
                pos += velocity * step;
                if (Math.abs(pos - targetPos) <= REST_POSITION && Math.abs(velocity) <= REST_VELOCITY) {
                    pos = targetPos;
                    velocity = 0.0;
                    phase = Phase.STABLE;
                }
            }
            case STABLE -> {
                pos = targetPos;
                velocity = 0.0;
            }
            case CLOSING -> {
                velocity -= config.closeAcceleration * step;
                pos += velocity * step;
                if (pos + extent < 0.0) {
                    pos = -extent;
                    velocity = 0.0;
                    phase = Phase.DONE;
                }
            }
            default -> {
                // IDLE / DONE: nothing to integrate.
            }
        }
    }

    public double pos() {
        return pos;
    }

    public double velocity() {
        return velocity;
    }

    public double targetPos() {
        return targetPos;
    }

    public Phase phase() {
        return phase;
    }

    public float impactFlash() {
        return impactFlash;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isClosing() {
        return phase == Phase.CLOSING;
    }

    public boolean isFinishedClosing() {
        return phase == Phase.DONE;
    }

    /**
     * Whether slot interaction must be suppressed. True while closing (and once done)
     * to prevent mis-clicks on a moving panel.
     */
    public boolean blocksInput() {
        return phase == Phase.CLOSING || phase == Phase.DONE;
    }
}
