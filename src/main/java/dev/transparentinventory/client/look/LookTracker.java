package dev.transparentinventory.client.look;

/**
 * Frame-rate independent smoother for the head/body angles of the player preview.
 *
 * <p>Uses the exponential form {@code alpha = 1 - exp(-speed * dt)} so that the
 * smoothing behaves identically at 30, 60, 144 and 240 FPS. The very first update
 * snaps to the target so the player does not visibly glide into place on open.
 */
public final class LookTracker {

    private boolean primed;

    private float headYaw;
    private float headPitch;
    private float bodyYaw;

    /** Forces the next update to snap directly to the requested angles. */
    public void reset() {
        primed = false;
    }

    /**
     * Smooths the current angles towards the requested targets.
     *
     * @param targetHeadYaw   desired head yaw in degrees
     * @param targetHeadPitch desired head pitch in degrees
     * @param targetBodyYaw   desired body yaw in degrees
     * @param dt              seconds since the previous update
     * @param smoothing       exponential smoothing speed; higher is snappier
     */
    public void update(float targetHeadYaw, float targetHeadPitch, float targetBodyYaw,
                       double dt, double smoothing) {
        if (!primed || dt <= 0.0) {
            headYaw = targetHeadYaw;
            headPitch = targetHeadPitch;
            bodyYaw = targetBodyYaw;
            primed = true;
            return;
        }
        double alpha = 1.0 - Math.exp(-Math.max(0.0, smoothing) * dt);
        alpha = Math.max(0.0, Math.min(1.0, alpha));
        float a = (float) alpha;
        headYaw += (targetHeadYaw - headYaw) * a;
        headPitch += (targetHeadPitch - headPitch) * a;
        bodyYaw += (targetBodyYaw - bodyYaw) * a;
    }

    public float headYaw() {
        return headYaw;
    }

    public float headPitch() {
        return headPitch;
    }

    public float bodyYaw() {
        return bodyYaw;
    }
}
