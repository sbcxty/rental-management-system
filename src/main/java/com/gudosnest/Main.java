package com.gudosnest;

import com.gudosnest.frontend.ComponentPreview;
import com.gudosnest.frontend.theme.Theme;

import javax.swing.SwingUtilities;

/**
 * Starts Gudo's Nest.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            // Phase 1: show the shared components. Phase 2 replaces this with the login screen.
            ComponentPreview.show();
        });
    }
}
