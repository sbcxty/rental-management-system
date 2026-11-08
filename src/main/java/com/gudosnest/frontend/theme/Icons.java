package com.gudosnest.frontend.theme;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.awt.Color;

/**
 * Loads the bundled line icons (resources/icons/*.svg, from the Lucide set, ISC license).
 * The SVG files are drawn in black; the color filter repaints them in any theme color.
 */
public final class Icons {

    private Icons() {
    }

    public static FlatSVGIcon get(String name, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon("icons/" + name + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        return icon;
    }

    public static FlatSVGIcon get(String name, Color color) {
        return get(name, 20, color);
    }
}
