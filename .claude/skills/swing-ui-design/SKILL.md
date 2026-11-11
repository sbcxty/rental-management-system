---
name: swing-ui-design
description: Use when designing, building, or restyling Java Swing screens, forms, dashboards, or dialogs, especially separate role-based screens such as owner and renter, or when a Swing UI looks dated, cramped, inconsistent, or uses null layout / setBounds.
---

# Swing UI Design

## Overview

Modern-looking Swing comes from a few enforced constants, not per-screen taste: FlatLaf for rendering, MigLayout for placement, one spacing grid, one accent color, one font family. Every screen, owner or renter, pulls from the same `Theme` and base components.

## Rules

1. **FlatLaf** is the only look and feel. Never use Metal/Nimbus or hand-painted borders.
2. **MigLayout** for every container. No `setLayout(null)`, no `setBounds`, no `setLocation`. If you reach for absolute positioning, you need a MigLayout constraint instead.
3. **8px grid.** Every gap, inset, and fixed size is a multiple of 8 (4 allowed only for tight icon/label pairs). Define constants; never type raw numbers in screens.
4. **One accent color.** Used only for primary actions, selection, focus, and active nav. Everything else is neutral gray/white. Status colors (error/success) are semantic, not accents, and used sparingly.
5. **Consistent fonts.** One family, 4 sizes max, set globally via `UIManager`. Never call `new Font(...)` inside a screen.
6. **Clear hierarchy.** One page title per screen, then section headings, then body. One primary button per view; secondary actions are outlined or plain.
7. **Separate owner and renter screens, shared style.** Distinct packages and classes per role; shared `ui.theme` and `ui.components`. Roles differ in content and navigation, never in look.

## Structure

```
ui/
  theme/    Theme.java, Spacing.java        // the only place style constants live
  common/   PageHeader, Card, FormRow, PrimaryButton, DataTable helpers
  owner/    OwnerShell, OwnerDashboardPanel, PropertiesPanel, TenantsPanel ...
  renter/   RenterShell, RenterHomePanel, MyLeasePanel, PaymentsPanel ...
```

Owner and renter code must not import each other. Shared needs go in `common`.

## Theme setup (call once, before creating any component)

```java
public final class Theme {
    public static final String ACCENT = "#2563EB";   // the one accent
    public static final String FONT_FAMILY = "Segoe UI";

    public static void install() {
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", ACCENT));
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font(FONT_FAMILY, Font.PLAIN, 14));
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.thumbArc", 999);
    }
}

public final class Spacing {
    public static final int XS = 4, S = 8, M = 16, L = 24, XL = 32, XXL = 48;
}
```

Call `Theme.install()` first in `main`, inside `SwingUtilities.invokeLater`.

## Type scale

| Role | Size / weight | How |
|---|---|---|
| Page title | 24 bold | `FlatLaf.styleClass` = `h1` or `putClientProperty` + derived font |
| Section heading | 18 semibold/bold | `h3` |
| Body / inputs / buttons | 14 regular | default font |
| Caption / hint | 12, muted gray | `small` + `Label.disabledForeground` |

Use `label.putClientProperty("FlatLaf.styleClass", "h1")` rather than creating fonts.

## Layout pattern (MigLayout)

```java
// Page: 24px outer inset, 16px between sections, content grows
setLayout(new MigLayout("insets 24, gap 16, fillx", "[grow]", "[]16[]16[grow]"));
add(new PageHeader("Properties", "Manage your listings"), "growx, wrap");
add(statsRow(), "growx, wrap");
add(tableCard, "grow");

// Form: label column fixed, field column grows, 8px row gap
JPanel form = new JPanel(new MigLayout("insets 0, gap 8 8, fillx", "[120!]16[grow]"));
form.add(new JLabel("Rent (monthly)"));
form.add(rentField, "growx, wrap");

// Button row: primary right-aligned, 8px apart
add(cancelBtn, "split 2, tag cancel");
add(saveBtn, "tag ok");
```

Prefer `growx`, `grow`, `wrap`, `sg` (size groups) and `push` over fixed pixel sizes. If a size is fixed, make it a multiple of 8 (e.g. `"w 240!"`, `"h 40!"`).

## Buttons and emphasis

- Primary: `rootPane.setDefaultButton(btn)` (FlatLaf paints it in the accent color) or `btn.putClientProperty("JButton.buttonType", "roundRect")` with accent background via style class.
- Secondary: default look. Destructive: same as secondary plus a confirm dialog; do not add a second accent color.
- Control height 40px (5 x 8) for buttons and text fields.

## Owner vs renter

| | Owner | Renter |
|---|---|---|
| Entry | `OwnerShell` | `RenterShell` |
| Nav | Dashboard, Properties, Tenants, Payments, Reports | Home, My Lease, Pay Rent, Requests |
| Focus | Portfolio overview, management tables | Personal status, one next action |

Both shells share one layout: left sidebar (fixed `w 240!`) + content area, built by a common `AppShell` base taking a nav list. Active nav item uses the accent; inactive is neutral. Login routes to the shell by role.

## Common Mistakes

| Mistake | Fix |
|---|---|
| `setBounds` / null layout "just for this panel" | MigLayout with `pos` is still wrong; use grid constraints |
| Gaps like 5, 10, 15, 20 | Snap to 4/8/16/24/32 |
| Several colored buttons (green save, red delete, blue add) | One accent for primary; others neutral |
| `new Font("Arial", ...)` in a screen | Style class or `Theme` |
| Setting LAF after creating components | Install first, or call `FlatLaf.updateUI()` |
| Owner and renter screens copy-pasted with tweaks | Extract to `common`, parametrize content |
| Every element bold or large | One title, then step down; whitespace separates groups |
| Dense tables edge to edge | Wrap in a Card with 16 inset, row height 40 |

## Dependencies

`com.formdev:flatlaf` and `com.miglayout:miglayout-swing`. Check the project's `pom.xml` / `build.gradle` for current versions before adding.
