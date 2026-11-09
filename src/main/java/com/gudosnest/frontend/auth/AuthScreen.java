package com.gudosnest.frontend.auth;

import com.formdev.flatlaf.FlatClientProperties;
import com.gudosnest.backend.database.DataException;
import com.gudosnest.backend.model.User;
import com.gudosnest.backend.service.AuthService;
import com.gudosnest.backend.service.RuleException;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.components.Widgets;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;
import com.gudosnest.frontend.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Login and sign-up (Stitch "Login & Sign Up"): the illustration on the left, and a form on the
 * right with "Sign up" and "Log in" tabs. Successful login or sign-up hands the user to onSuccess.
 */
public class AuthScreen extends JPanel {

    private final AuthService auth = new AuthService();
    private final Consumer<User> onSuccess;
    private final JPanel formArea = Ui.transparent("insets 0, fill", "[grow,fill]");
    private final UnderlineTab signUpTab = new UnderlineTab("Sign up");
    private final UnderlineTab logInTab = new UnderlineTab("Log in");

    public AuthScreen(Consumer<User> onSuccess) {
        super(new MigLayout("insets 0, fill, gap 0", "[50%,fill][50%,fill]", "[grow,fill]"));
        this.onSuccess = onSuccess;
        setBackground(Colors.CARD);
        add(artPanel());
        add(formPanel());
        showLogIn();
    }

    private JComponent artPanel() {
        JPanel art = new JPanel(new MigLayout("insets 48 56 48 56, fill, gap 0, wrap 1", "[grow,fill]", "[][grow][]"));
        art.setBackground(new java.awt.Color(0xF1EEF4));
        art.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Colors.BORDER));

        JPanel brand = Ui.transparent("insets 0, gap 12", "[][]");
        JLabel logo = new JLabel(Icons.get("house", 20, Colors.TEXT_ON_DARK), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Colors.BURGUNDY);
                int arc = com.formdev.flatlaf.util.UIScale.scale(10);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        brand.add(logo, "w 40!, h 40!");
        brand.add(Ui.label("Gudo's Nest", Fonts.logo().deriveFont(24f), Colors.TEXT));
        art.add(brand);

        JPanel middle = Ui.transparent("insets 0, gap 0, wrap 1, fill", "[grow,fill]");
        JLabel headline = Ui.label("One app for renters and owners", Fonts.sectionTitle(), Colors.TEXT);
        headline.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel sub = Ui.label("Book and manage properties with ease", Fonts.body(), Colors.TEXT_MUTED);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        middle.add(headline, "gaptop push");
        middle.add(sub, "gaptop 4, gapbottom 32");
        middle.add(new LoginArt(), "grow, push, hmin 240, wmin 200");
        art.add(middle, "grow");
        return art;
    }

    private JComponent formPanel() {
        JPanel right = new JPanel(new MigLayout("insets 48, fill", "[center]", "[center]"));
        right.setBackground(Colors.CARD);
        JPanel column = Ui.transparent("insets 0, gap 0, wrap 1", "[440!,fill]");

        JPanel tabs = Ui.transparent("insets 0, gap 0, fillx", "[50%,fill][50%,fill]");
        tabs.add(signUpTab);
        tabs.add(logInTab);
        signUpTab.onClick(this::showSignUp);
        logInTab.onClick(this::showLogIn);
        column.add(tabs, "gapbottom 32");
        column.add(formArea);
        right.add(column);
        return right;
    }

    // ----------------------------------------------------------------------------- log in

    private void showLogIn() {
        signUpTab.setSelected(false);
        logInTab.setSelected(true);
        JPanel form = Ui.transparent("insets 0, gap 0, wrap 1, fillx", "[grow,fill]");
        form.add(Ui.label("Welcome back", Fonts.pageTitle(), Colors.TEXT));
        form.add(Ui.label("Log in to see your stays and properties.", Fonts.body(), Colors.TEXT_MUTED), "gapbottom 24");

        JTextField email = Ui.textField("name@example.com");
        JPasswordField password = passwordField();
        JLabel error = Ui.error("");
        error.setVisible(false);
        form.add(Ui.field("Email address", email), "gapbottom 16");
        form.add(Ui.field("Password", password), "gapbottom 16");
        form.add(error, "gapbottom 12, hidemode 3");

        AppButton logIn = new AppButton("Log in", AppButton.Style.PRIMARY);
        Runnable submit = () -> {
            try {
                onSuccess.accept(auth.logIn(email.getText(), new String(password.getPassword())));
            } catch (RuleException | DataException ex) {
                showError(error, ex.getMessage());
                password.setText("");
                password.requestFocusInWindow();
            }
        };
        logIn.addActionListener(e -> submit.run());
        email.addActionListener(e -> password.requestFocusInWindow());
        password.addActionListener(e -> submit.run());
        form.add(logIn, "h 44!, gapbottom 24");
        form.add(footer("New to Gudo's Nest?", "Create an account", this::showSignUp));
        setForm(form);
        email.requestFocusInWindow();
    }

    // ---------------------------------------------------------------------------- sign up

    private void showSignUp() {
        signUpTab.setSelected(true);
        logInTab.setSelected(false);
        JPanel form = Ui.transparent("insets 0, gap 0, wrap 1, fillx", "[grow,fill]");
        form.add(Ui.label("Create account", Fonts.pageTitle(), Colors.TEXT));
        form.add(Ui.label("One account for staying and hosting.", Fonts.body(), Colors.TEXT_MUTED), "gapbottom 24");

        JTextField name = Ui.textField("Your full name");
        JTextField email = Ui.textField("name@example.com");
        JPasswordField password = passwordField();
        password.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "At least " + AuthService.MIN_PASSWORD_LENGTH + " characters");
        form.add(Ui.field("Full name", name), "gapbottom 16");
        form.add(Ui.field("Email address", email), "gapbottom 16");
        form.add(Ui.field("Password", password), "gapbottom 20");

        form.add(Ui.label("Account type", Fonts.bodyMedium(), Colors.TEXT));
        form.add(Ui.caption("Choose one or both. You can switch between them after logging in."), "gapbottom 8");
        RoleOption renter = new RoleOption("Renter", "Find places to stay, book and pay, and manage your bookings.");
        RoleOption owner = new RoleOption("Owner", "List properties, approve bookings, and track income.");
        renter.setSelected(true);
        form.add(renter, "gapbottom 8");
        form.add(owner, "gapbottom 16");

        JLabel error = Ui.error("");
        error.setVisible(false);
        form.add(error, "gapbottom 12, hidemode 3");
        AppButton create = new AppButton("Create account", AppButton.Style.PRIMARY);
        Runnable submit = () -> {
            try {
                onSuccess.accept(auth.signUp(name.getText(), email.getText(), new String(password.getPassword()),
                        owner.isSelected(), renter.isSelected()));
            } catch (RuleException | DataException ex) {
                showError(error, ex.getMessage());
            }
        };
        create.addActionListener(e -> submit.run());
        password.addActionListener(e -> submit.run());
        form.add(create, "h 44!, gapbottom 24");
        form.add(footer("Already have an account?", "Log in", this::showLogIn));
        setForm(form);
        name.requestFocusInWindow();
    }

    // ---------------------------------------------------------------------------- helpers

    private void setForm(JComponent form) {
        formArea.removeAll();
        formArea.add(form);
        formArea.revalidate();
        formArea.repaint();
    }

    private static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        field.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");
        return field;
    }

    private static void showError(JLabel error, String message) {
        error.setText(message);
        error.setVisible(true);
        error.getParent().revalidate();
    }

    private static JComponent footer(String question, String linkText, Runnable action) {
        JPanel footer = Ui.transparent("insets 16 0 0 0, fillx", "[grow][]");
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.BORDER));
        footer.add(Ui.muted(question));
        footer.add(Widgets.link(linkText, false, action));
        return footer;
    }

    /** "Sign up" / "Log in" tab with a Primary underline when selected. */
    private static final class UnderlineTab extends JLabel {
        private boolean selected;

        UnderlineTab(String text) {
            super(text, SwingConstants.CENTER);
            setFont(Fonts.body(16f, true));
            setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void onClick(Runnable action) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            setForeground(selected ? Colors.TEXT : Colors.TEXT_MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int line = com.formdev.flatlaf.util.UIScale.scale(selected ? 2 : 1);
            g.setColor(selected ? Colors.PRIMARY : Colors.BORDER);
            g.fillRect(0, getHeight() - line, getWidth(), line);
        }
    }

    /** A bordered box with a checkbox, a title and one line of description. */
    private static final class RoleOption extends JPanel {
        private final JCheckBox box = new JCheckBox();

        RoleOption(String title, String description) {
            super(new MigLayout("insets 12 14 12 14, gap 0 2", "[]12[grow,fill]", "[][]"));
            setBackground(Colors.CARD);
            setBorder(new com.formdev.flatlaf.ui.FlatLineBorder(new java.awt.Insets(1, 1, 1, 1), Colors.BORDER, 1f, Theme.CONTROL_RADIUS));
            putClientProperty(FlatClientProperties.STYLE, "arc:" + Theme.CONTROL_RADIUS);
            box.setOpaque(false);
            add(box, "spany 2, aligny top");
            add(Ui.label(title, Fonts.bodyMedium(), Colors.TEXT), "wrap");
            add(Ui.label(description, Fonts.caption(), Colors.TEXT_MUTED), "wmin 0");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    box.setSelected(!box.isSelected());
                }
            });
        }

        boolean isSelected() {
            return box.isSelected();
        }

        void setSelected(boolean selected) {
            box.setSelected(selected);
        }
    }
}
