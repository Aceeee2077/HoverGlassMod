package dev.transparentinventory.client.screen;

/**
 * Ordering and titles of the F8 settings pages.
 *
 * <p>Pure Java so the page cycle can be tested without a running client. The
 * page index is always kept in {@code [0, COUNT)}; the previous/next helpers
 * wrap around so the settings screen never lands on a blank page.
 */
public final class SettingsPages {

    public static final int COUNT = 12;
    /** Index of the compatibility page, which also owns the shortcut buttons. */
    public static final int COMPATIBILITY = 9;
    /** Advanced framing offsets, kept after compatibility so existing pages stay put. */
    public static final int CAMERA_ANGLES = 10;
    /** Alpha values of the glass material itself. */
    public static final int GLASS_MATERIAL = 11;

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
            "spatialui.settings.glass_material"
    };

    private SettingsPages() {
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
