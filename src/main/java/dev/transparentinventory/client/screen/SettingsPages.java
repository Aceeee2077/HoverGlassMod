package dev.transparentinventory.client.screen;

/**
 * Ordering and titles of the F8 settings pages.
 *
 * <p>Pure Java so the page cycle can be tested without a running client. The
 * page index is always kept in {@code [0, COUNT)}; the previous/next helpers
 * wrap around so the settings screen never lands on a blank page.
 */
public final class SettingsPages {

    public static final int COUNT = 13;
    /** Index of the compatibility page, which also owns the shortcut buttons. */
    public static final int COMPATIBILITY = 9;
    /** Advanced framing offsets, kept after compatibility so existing pages stay put. */
    public static final int CAMERA_ANGLES = 10;
    /** Alpha values of the glass material itself. */
    public static final int GLASS_MATERIAL = 11;
    /** Which screens the spatial UI applies to, and how it degrades. */
    public static final int SCREEN_SUPPORT = 12;

    private static final String[] TITLES = {
            "spatialui.settings.opacity",
            "spatialui.settings.more",
            "spatialui.settings.entrance",
            "spatialui.settings.look",
            "spatialui.settings.hotbar",
            "spatialui.settings.motion",
            "spatialui.settings.camera_motion",
            "spatialui.settings.world_effects",
            "spatialui.settings.player_motion",
            "spatialui.settings.compatibility",
            "spatialui.settings.camera_angles",
            "spatialui.settings.glass_material",
            "spatialui.settings.screen_support"
    };

    /** Last page opened this session; 0 until the screen navigates somewhere else. */
    private static int remembered;

    private SettingsPages() {
    }

    /** The page index that should be shown when the settings screen opens. */
    public static int remembered() {
        return clamp(remembered);
    }

    /** Stores the page for the next open, keeping it inside the valid range. */
    public static int remember(int page) {
        remembered = clamp(page);
        return remembered;
    }

    /** Index of the last page, i.e. where End jumps. */
    public static int last() {
        return COUNT - 1;
    }

    /** Keeps an arbitrary index inside the page range; invalid values start over at the first page. */
    public static int clamp(int page) {
        return page >= 0 && page < COUNT ? page : 0;
    }

    /** The page after this one, wrapping back to the first page. */
    public static int next(int page) {
        return (clamp(page) + 1) % COUNT;
    }

    /** The page before this one, wrapping to the last page. */
    public static int previous(int page) {
        return (clamp(page) + COUNT - 1) % COUNT;
    }

    /** The page a number of steps away, wrapping in both directions. */
    public static int shift(int page, int steps) {
        return Math.floorMod(clamp(page) + steps, COUNT);
    }

    /**
     * Page direction for a vertical scroll amount: scrolling up goes to the
     * previous page, scrolling down to the next one. Sub-notch and invalid
     * input is ignored so trackpad jitter cannot flip pages.
     */
    public static int scrollStep(double scrollY) {
        if (!Double.isFinite(scrollY) || Math.abs(scrollY) < 0.5) return 0;
        return scrollY > 0 ? -1 : 1;
    }

    /** Translation key of a page's own title. */
    public static String titleKey(int page) {
        return TITLES[clamp(page)];
    }

    /** Translation key shown on the "next" button, i.e. the upcoming page's title. */
    public static String nextLabelKey(int page) {
        return titleKey(next(page));
    }

    /** Translation key shown on the "previous" button, i.e. the page it returns to. */
    public static String previousLabelKey(int page) {
        return titleKey(previous(page));
    }
}
