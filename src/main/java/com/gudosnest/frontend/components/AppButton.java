package com.gudosnest.frontend.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import com.gudosnest.frontend.theme.Theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;

/**
 * The app's buttons. Use one PRIMARY button per view.
 *   PRIMARY   - Primary fill, white text (main action)
 *   SECONDARY - Secondary fill, white text
 *   INVERTED  - light Primary fill, Primary text (quiet actions such as "Review booking")
 *   OUTLINED  - white with a Primary outline
 *   DANGER    - muted red fill, only for destructive actions (Delete, Reject)
 *   TEXT      - no background, Primary text (links such as "View all")
 */
public class AppButton extends JButton {

    public enum Style { PRIMARY, SECONDARY, INVERTED, OUTLINED, DANGER, TEXT }

    private final Style style;

    public AppButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(Fonts.bodyMedium());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusPainted(false);
        putClientProperty(FlatClientProperties.STYLE, styleFor(style));
    }

    /** Adds a line icon in the button's text color. Returns this for chaining. */
    public AppButton withIcon(String iconName) {
        setIcon(Icons.get(iconName, 16, textColor()));
        setIconTextGap(Theme.S);
        return this;
    }

    private Color textColor() {
        return switch (style) {
            case PRIMARY, SECONDARY, DANGER -> Colors.TEXT_ON_DARK;
            case INVERTED, OUTLINED, TEXT -> Colors.PRIMARY;
        };
    }

    private static String styleFor(Style style) {
        String common = "arc:" + Theme.CONTROL_RADIUS + ";focusWidth:0;innerFocusWidth:0;margin:8,16,8,16;";
        return common + switch (style) {
            case PRIMARY -> fill(Colors.PRIMARY, Colors.PRIMARY_HOVER, Colors.TEXT_ON_DARK);
            case SECONDARY -> fill(Colors.SECONDARY, Colors.SECONDARY_HOVER, Colors.TEXT_ON_DARK);
            case DANGER -> fill(Colors.DANGER, Colors.DANGER_HOVER, Colors.TEXT_ON_DARK);
            case INVERTED -> fill(Colors.PRIMARY_TINT, Colors.BORDER, Colors.PRIMARY);
            case OUTLINED -> "borderWidth:1;borderColor:" + Colors.hex(Colors.PRIMARY)
                    + ";hoverBorderColor:" + Colors.hex(Colors.PRIMARY)
                    + ";focusedBorderColor:" + Colors.hex(Colors.PRIMARY)
                    + ";background:#FFFFFF;hoverBackground:" + Colors.hex(Colors.PRIMARY_TINT)
                    + ";pressedBackground:" + Colors.hex(Colors.PRIMARY_TINT)
                    + ";foreground:" + Colors.hex(Colors.PRIMARY);
            case TEXT -> "borderWidth:0;background:null;hoverBackground:" + Colors.hex(Colors.PRIMARY_TINT)
                    + ";pressedBackground:" + Colors.hex(Colors.PRIMARY_TINT)
                    + ";foreground:" + Colors.hex(Colors.PRIMARY) + ";margin:4,8,4,8";
        };
    }

    private static String fill(Color bg, Color hover, Color fg) {
        return "borderWidth:0;background:" + Colors.hex(bg)
                + ";hoverBackground:" + Colors.hex(hover)
                + ";pressedBackground:" + Colors.hex(hover)
                + ";focusedBackground:" + Colors.hex(bg)
                + ";foreground:" + Colors.hex(fg)
                + ";disabledBackground:" + Colors.hex(Colors.BORDER)
                + ";disabledText:" + Colors.hex(Colors.NEUTRAL);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (style != Style.TEXT) {
            size.height = Theme.CONTROL_HEIGHT;
        }
        return size;
    }
}
