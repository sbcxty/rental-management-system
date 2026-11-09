package com.gudosnest.frontend;

import com.gudosnest.backend.database.DataException;
import com.gudosnest.backend.database.SessionManager;
import com.gudosnest.backend.model.User;
import com.gudosnest.backend.service.BookingService;
import com.gudosnest.frontend.auth.AuthScreen;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Icons;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** The one application window. Shows the login screen or a portal. */
public class MainWindow extends JFrame {

    public MainWindow() {
        super("Gudo's Nest");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1440, 900);
        setIconImages(appIcons());
        setLocationRelativeTo(null);
        showAuth();
    }

    public void showAuth() {
        setContentPane(new AuthScreen(this::loggedIn));
        revalidate();
        repaint();
    }

    /** People with both roles start in the Owner portal (they can switch from the avatar menu). */
    private void loggedIn(User user) {
        showPortal(user, user.isOwner() ? SessionManager.Portal.OWNER : SessionManager.Portal.RENTER);
    }

    public void showPortal(User user, SessionManager.Portal kind) {
        SessionManager.getInstance().logIn(user, kind);
        try {
            new BookingService().refreshStatuses();
        } catch (DataException ignored) {
            // the pages will show the database problem
        }
        Portal portal = Portal.create(this, user, kind);
        setContentPane(portal.build());
        revalidate();
        repaint();
    }

    public void logOut() {
        SessionManager.getInstance().logOut();
        showAuth();
    }

    /** The Burgundy house square, drawn at the sizes Windows asks for. */
    private static List<Image> appIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 24, 32, 48, 64}) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Colors.BURGUNDY);
            g.fillRoundRect(0, 0, size, size, size / 3, size / 3);
            int iconSize = Math.round(size * 0.6f);
            g.drawImage(Icons.get("house", iconSize, Colors.TEXT_ON_DARK).getImage(),
                    (size - iconSize) / 2, (size - iconSize) / 2, null);
            g.dispose();
            icons.add(img);
        }
        return icons;
    }
}
