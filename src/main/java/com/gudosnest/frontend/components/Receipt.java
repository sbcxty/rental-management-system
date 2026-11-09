package com.gudosnest.frontend.components;

import com.gudosnest.backend.model.Booking;
import com.gudosnest.backend.model.Records.DamageReport;
import com.gudosnest.backend.service.pricing.Price;
import com.gudosnest.frontend.theme.Colors;
import com.gudosnest.frontend.theme.Fonts;
import com.gudosnest.frontend.theme.Icons;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A receipt image drawn with Java2D from the saved booking data (never typed in by hand).
 * The same drawing is shown on screen and saved as a PNG, so they always match.
 */
public final class Receipt {

    private static final int WIDTH = 520;

    /** One printed line: label on the left, amount on the right. */
    private record Line(String label, String value, boolean strong) {
    }

    private final String title;
    private final String reference;
    private final LocalDateTime date;
    private final List<String[]> details = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private String totalLabel;
    private String totalValue;

    private Receipt(String title, String reference, LocalDateTime date) {
        this.title = title;
        this.reference = reference;
        this.date = date;
    }

    /** Receipt for the payment made when booking. */
    public static Receipt forPayment(Booking b) {
        Receipt r = new Receipt("Payment receipt", Formats.ref(b.getId(), b.getCreatedAt()), b.getPaidAt());
        r.stayDetails(b);
        for (Price.Line line : Price.forStay(b.getNightlyRate(), b.getNights(), b.getCleaningFee()).lines()) {
            r.lines.add(new Line(line.label(), (line.discount() ? "-" : "") + Formats.money(line.amount()), false));
        }
        r.totalLabel = "Total paid";
        r.totalValue = Formats.money(b.getTotal());
        return r;
    }

    /** Receipt for a refund paid back to the renter. */
    public static Receipt forRefund(Booking b, LocalDateTime processedAt) {
        Receipt r = new Receipt("Refund receipt", Formats.ref(b.getId(), b.getCreatedAt()), processedAt);
        r.stayDetails(b);
        r.lines.add(new Line("Amount paid", Formats.money(b.getTotal()), false));
        BigDecimal fee = b.getCancellationFee() == null ? BigDecimal.ZERO : b.getCancellationFee();
        if (fee.signum() > 0) {
            r.lines.add(new Line("Cancellation fee (20%)", "-" + Formats.money(fee), false));
        }
        r.totalLabel = "Total refunded";
        r.totalValue = Formats.money(b.getRefundAmount());
        return r;
    }

    /** Receipt for a damage fee charged after a stay. */
    public static Receipt forDamageFee(DamageReport report, LocalDateTime paidAt) {
        Booking b = report.booking();
        Receipt r = new Receipt("Damage fee receipt", Formats.ref(b.getId(), b.getCreatedAt()), paidAt);
        r.stayDetails(b);
        r.lines.add(new Line(report.description(), Formats.money(report.requestedFee()), false));
        r.totalLabel = "Total paid";
        r.totalValue = Formats.money(report.requestedFee());
        return r;
    }

    private void stayDetails(Booking b) {
        details.add(new String[]{"Renter", b.getRenterName()});
        details.add(new String[]{"Property", b.getPropertyName() + " · " + b.getUnitLabel()});
        details.add(new String[]{"Stay", Formats.dateRange(b.getCheckIn().toLocalDate(), b.getCheckOut().toLocalDate())
                + " · " + Formats.nights(b.getNights())});
        details.add(new String[]{"Guests", Formats.guests(b.getGuests())});
    }

    /** Draws the receipt at 2× so it stays sharp when saved or scaled. */
    public BufferedImage render() {
        int scale = 2;
        int height = 250 + details.size() * 24 + lines.size() * 28 + 90;
        BufferedImage img = new BufferedImage(WIDTH * scale, height * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(Colors.CARD);
        g.fillRect(0, 0, WIDTH, height);

        int pad = 32;
        int y = pad;
        // Brand
        g.setColor(Colors.BURGUNDY);
        g.fillRoundRect(pad, y, 36, 36, 10, 10);
        Icon house = Icons.get("house", 18, Colors.TEXT_ON_DARK);
        house.paintIcon(null, g, pad + 9, y + 9);
        g.setFont(Fonts.logo().deriveFont(17f));
        g.setColor(Colors.TEXT);
        g.drawString("Gudo's Nest", pad + 48, y + 24);
        y += 64;

        g.setFont(Fonts.sectionTitle().deriveFont(22f));
        g.drawString(title, pad, y);
        y += 24;
        g.setFont(Fonts.body().deriveFont(13f));
        g.setColor(Colors.TEXT_MUTED);
        g.drawString(reference + "  ·  " + Formats.dateTime(date), pad, y);
        y += 24;
        divider(g, pad, y);
        y += 28;

        for (String[] d : details) {
            g.setFont(Fonts.body().deriveFont(13f));
            g.setColor(Colors.TEXT_MUTED);
            g.drawString(d[0], pad, y);
            g.setColor(Colors.TEXT);
            g.drawString(fit(g, d[1], WIDTH - pad * 2 - 110), pad + 110, y);
            y += 24;
        }
        y += 6;
        divider(g, pad, y);
        y += 28;

        for (Line line : lines) {
            drawLine(g, line.label(), line.value(), pad, y, Fonts.body().deriveFont(13f));
            y += 28;
        }
        divider(g, pad, y - 8);
        y += 24;
        drawLine(g, totalLabel, totalValue, pad, y, Fonts.sectionTitle().deriveFont(18f));
        y += 28;
        g.setFont(Fonts.caption().deriveFont(11f));
        g.setColor(Colors.TEXT_MUTED);
        g.drawString("Keep this receipt for your records.", pad, y);
        g.dispose();
        return img;
    }

    private static void drawLine(Graphics2D g, String label, String value, int pad, int y, Font font) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Colors.TEXT);
        int valueW = fm.stringWidth(value);
        g.drawString(fit(g, label, WIDTH - pad * 2 - valueW - 16), pad, y);
        g.drawString(value, WIDTH - pad - valueW, y);
    }

    private static void divider(Graphics2D g, int pad, int y) {
        g.setColor(Colors.BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(pad, y, WIDTH - pad, y);
    }

    /** Shortens text with "…" so it fits the given width. */
    private static String fit(Graphics2D g, String text, int width) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "…") > width) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }

    /** Saves the receipt as a PNG, creating the folder if needed. */
    public void save(File file) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create the folder " + parent);
        }
        ImageIO.write(render(), "png", file);
    }

    /** Saves to the path stored in the database (receipts/...), ignoring failures (it can be redrawn). */
    public void saveQuietly(String path) {
        try {
            save(new File(path));
        } catch (IOException ignored) {
            // the receipt is always drawn again from the data when viewed
        }
    }
}
