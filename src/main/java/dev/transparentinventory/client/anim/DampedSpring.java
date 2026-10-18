package dev.transparentinventory.client.anim;

/** Exact underdamped solution, including velocity; frame partition does not change the result. */
public final class DampedSpring {
    private double position, velocity;
    public void impulse(double amount) { if (Double.isFinite(amount)) velocity = Math.max(-2, Math.min(2, velocity + amount)); }
    public void update(double dt) {
        if (!Double.isFinite(dt) || dt <= 0) return;
        double damping = 14, omega = 20;
        double a = position, b = (velocity + damping * position) / omega;
        double sin = Math.sin(omega * dt), cos = Math.cos(omega * dt), decay = Math.exp(-damping * dt);
        position = decay * (a * cos + b * sin);
        velocity = decay * ((b * omega - damping * a) * cos - (a * omega + damping * b) * sin);
        if (Math.abs(position) < 1E-7 && Math.abs(velocity) < 1E-6) reset();
    }
    public double position() { return position; }
    public double velocity() { return velocity; }
    public void reset() { position = velocity = 0; }
}
