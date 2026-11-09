package com.gudosnest.frontend.theme;

import javax.swing.text.StyleContext;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

/**
 * The only place fonts are defined.
 * Plus Jakarta Sans for headings, DM Sans for everything else.
 */
public final class Fonts {

    private Fonts() {
    }

    private static Font jakartaSemiBold;
    private static Font jakartaBold;
    private static String dmRegularFamily;
    private static String dmMediumFamily;

    /** Loads the bundled TTF files. Called once by Theme.install(). */
    static void load() {
        jakartaSemiBold = loadFont("PlusJakartaSans-SemiBold.ttf");
        jakartaBold = loadFont("PlusJakartaSans-Bold.ttf");
        dmRegularFamily = loadFont("DMSans-Regular.ttf").getFamily();
        dmMediumFamily = loadFont("DMSans-Medium.ttf").getFamily();
        loadFont("DMSans-Bold.ttf");   // registered as the bold style of the DM Sans family
    }

    /**
     * DM Sans has no peso sign. StyleContext returns a "composite" font that uses DM Sans for
     * every character it has and falls back to a system font for the rest, so ₱ still shows.
     */
    private static Font dm(String family, int style, float size) {
        return StyleContext.getDefaultStyleContext().getFont(family, style, Math.round(size));
    }

    private static Font loadFont(String file) {
        try (InputStream in = Fonts.class.getResourceAsStream("/fonts/" + file)) {
            if (in == null) {
                throw new IllegalStateException("Missing font file: /fonts/" + file);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load font " + file, e);
        }
    }

    // Headings (Plus Jakarta Sans)
    public static Font pageTitle() {
        return jakartaBold.deriveFont(30f);
    }

    public static Font sectionTitle() {
        return jakartaSemiBold.deriveFont(18f);
    }

    public static Font cardTitle() {
        return jakartaSemiBold.deriveFont(16f);
    }

    public static Font statValue() {
        return jakartaBold.deriveFont(30f);
    }

    /** Small uppercase table header text with a little letter spacing. */
    public static Font tableHeader() {
        java.util.Map<java.awt.font.TextAttribute, Object> spacing =
                java.util.Map.of(java.awt.font.TextAttribute.TRACKING, 0.06f);
        return dm(dmMediumFamily, Font.PLAIN, 11).deriveFont(spacing);
    }

    public static Font logo() {
        return jakartaBold.deriveFont(17f);
    }

    // Body (DM Sans)
    public static Font body() {
        return dm(dmRegularFamily, Font.PLAIN, 14);
    }

    public static Font bodyMedium() {
        return dm(dmMediumFamily, Font.PLAIN, 14);
    }

    public static Font bodyBold() {
        return dm(dmRegularFamily, Font.BOLD, 14);
    }

    public static Font caption() {
        return dm(dmRegularFamily, Font.PLAIN, 12);
    }

    public static Font captionMedium() {
        return dm(dmMediumFamily, Font.PLAIN, 12);
    }

    public static Font large() {
        return dm(dmRegularFamily, Font.BOLD, 18);
    }

    /** Any DM Sans size, for special cases such as avatars. */
    public static Font body(float size, boolean medium) {
        return dm(medium ? dmMediumFamily : dmRegularFamily, Font.PLAIN, size);
    }
}
