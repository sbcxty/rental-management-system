package com.gudosnest.frontend.theme;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Insets;
import java.util.Map;

/**
 * Installs the look and feel. Call Theme.install() once, before creating any window.
 */
public final class Theme {

    private Theme() {
    }

    // Spacing grid (multiples of 8; 4 only for tight icon/label pairs)
    public static final int XS = 4;
    public static final int S = 8;
    public static final int M = 16;
    public static final int L = 24;
    public static final int XL = 32;
    public static final int XXL = 48;

    public static final int CONTROL_HEIGHT = 40;
    public static final int RADIUS = 12;          // cards and dialogs
    public static final int CONTROL_RADIUS = 8;   // buttons and inputs

    public static void install() {
        Fonts.load();

        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@accentColor", Colors.hex(Colors.PRIMARY),
                "@background", Colors.hex(Colors.PAGE),
                "@foreground", Colors.hex(Colors.TEXT)));
        FlatLightLaf.setup();

        UIManager.put("defaultFont", Fonts.body());
        UIManager.put("Component.arc", CONTROL_RADIUS);
        UIManager.put("Button.arc", CONTROL_RADIUS);
        UIManager.put("TextComponent.arc", CONTROL_RADIUS);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.focusColor", Colors.PRIMARY);
        UIManager.put("Component.focusedBorderColor", Colors.PRIMARY);
        UIManager.put("Component.borderColor", Colors.BORDER);
        UIManager.put("Button.borderColor", Colors.BORDER);
        UIManager.put("Button.focusedBorderColor", Colors.PRIMARY);
        UIManager.put("Button.default.borderWidth", 0);
        UIManager.put("Component.disabledBorderColor", Colors.BORDER);
        UIManager.put("TextComponent.background", Colors.CARD);
        UIManager.put("TextField.margin", new Insets(8, 12, 8, 12));
        UIManager.put("PasswordField.margin", new Insets(8, 12, 8, 12));
        UIManager.put("TextArea.margin", new Insets(8, 12, 8, 12));
        UIManager.put("ComboBox.padding", new Insets(4, 8, 4, 8));
        UIManager.put("ComboBox.background", Colors.CARD);
        UIManager.put("Spinner.background", Colors.CARD);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollPane.border", null);
        UIManager.put("Panel.background", Colors.PAGE);
        UIManager.put("Label.foreground", Colors.TEXT);
        UIManager.put("Label.disabledForeground", Colors.TEXT_MUTED);
        UIManager.put("PopupMenu.borderColor", Colors.BORDER);
        UIManager.put("PopupMenu.background", Colors.CARD);
        UIManager.put("MenuItem.selectionBackground", Colors.PRIMARY_TINT);
        UIManager.put("MenuItem.selectionForeground", Colors.TEXT);
        UIManager.put("List.selectionBackground", Colors.PRIMARY_TINT);
        UIManager.put("List.selectionForeground", Colors.TEXT);
        UIManager.put("ComboBox.selectionBackground", Colors.PRIMARY_TINT);
        UIManager.put("ComboBox.selectionForeground", Colors.TEXT);
        UIManager.put("Table.selectionBackground", Colors.PRIMARY_TINT);
        UIManager.put("Table.selectionForeground", Colors.TEXT);
        UIManager.put("Table.gridColor", Colors.BORDER);
        UIManager.put("TableHeader.background", Colors.TABLE_HEADER);
        UIManager.put("TableHeader.separatorColor", Colors.TABLE_HEADER);
        // Headers can't be clicked (no sorting), so they don't change colour under the mouse.
        UIManager.put("TableHeader.hoverBackground", Colors.TABLE_HEADER);
        UIManager.put("TableHeader.pressedBackground", Colors.TABLE_HEADER);
        UIManager.put("TableHeader.bottomSeparatorColor", Colors.BORDER);
        UIManager.put("ToolTip.background", Colors.BURGUNDY);
        UIManager.put("ToolTip.foreground", Colors.TEXT_ON_DARK);
        UIManager.put("ToolTip.border", javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        UIManager.put("CheckBox.icon.checkmarkColor", Colors.CARD);
        UIManager.put("CheckBox.icon.selectedBackground", Colors.PRIMARY);
        UIManager.put("CheckBox.icon.selectedBorderColor", Colors.PRIMARY);
        UIManager.put("OptionPane.background", Colors.CARD);
        UIManager.put("OptionPane.messageFont", Fonts.body());
    }
}
