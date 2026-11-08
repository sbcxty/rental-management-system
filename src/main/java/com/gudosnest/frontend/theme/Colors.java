package com.gudosnest.frontend.theme;

import java.awt.Color;

/**
 * The only place colors are defined. Flat solid colors, no gradients.
 */
public final class Colors {

    private Colors() {
    }

    // Palette
    public static final Color PRIMARY = new Color(0x593F62);        // primary buttons, active states, fee values
    public static final Color SECONDARY = new Color(0x7B6D8D);      // secondary buttons
    public static final Color TERTIARY = new Color(0x8499B1);       // charts, subtle tags
    public static final Color LIGHT_BLUE = new Color(0xA5C4D4);     // charts, subtle tags
    public static final Color BURGUNDY = new Color(0x36151E);       // sidebar, main text
    public static final Color PAGE = new Color(0xEAE7EE);           // page background
    public static final Color CARD = Color.WHITE;                   // cards
    public static final Color NEUTRAL = new Color(0x736C7C);        // secondary text
    public static final Color BORDER = new Color(0xE3DEE8);         // 1px borders
    public static final Color DANGER = new Color(0xA94B4B);         // muted red: Delete, Reject, errors only

    // Text
    public static final Color TEXT = BURGUNDY;
    public static final Color TEXT_MUTED = NEUTRAL;
    public static final Color TEXT_ON_DARK = Color.WHITE;

    // Light fills made from the palette (flat, for pills, hovers and selected rows)
    public static final Color PRIMARY_TINT = new Color(0xEFEAF2);   // light Primary fill
    public static final Color PRIMARY_HOVER = new Color(0x4A3352);  // Primary button hover
    public static final Color SECONDARY_HOVER = new Color(0x6A5D7B);
    public static final Color DANGER_HOVER = new Color(0x934040);
    public static final Color BLUE_TINT = new Color(0xE4EEF3);      // light Light Blue fill
    public static final Color SURFACE_HOVER = new Color(0xF6F4F8);  // row / item hover on white

    // Sidebar (Burgundy background)
    public static final Color SIDEBAR = BURGUNDY;
    public static final Color SIDEBAR_TEXT = new Color(0xD9D2DF);
    public static final Color SIDEBAR_HOVER = new Color(0x4A2731);
    public static final Color SIDEBAR_DIVIDER = new Color(0x4F2C37);
    public static final Color SIDEBAR_ACTIVE = PRIMARY;

    /** "#593F62" style string, used in FlatLaf style properties. */
    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
