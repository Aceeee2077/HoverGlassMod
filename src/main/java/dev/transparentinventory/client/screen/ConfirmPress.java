package dev.transparentinventory.client.screen;

/**
 * Two-step confirmation for buttons that replace or erase the whole config.
 *
 * <p>The first press only arms the action; the second press within the same
 * widget build runs it. Rebuilding the screen ({@link #disarm()}) throws the
 * pending press away, so a confirmation can never survive a page change or an
 * unrelated click. Pure Java so the rule is covered by the regression tests.
 */
public final class ConfirmPress {

    private boolean armed;

    /**
     * Records one press.
     *
     * @return true when this press completes the action, false when it only armed it
     */
    public boolean press() {
        if (!armed) {
            armed = true;
            return false;
        }
        armed = false;
        return true;
    }

    /** True while the action is waiting for its confirming press. */
    public boolean armed() {
        return armed;
    }

    /** Drops a pending confirmation. */
    public void disarm() {
        armed = false;
    }
}
