package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loading, scaling and saving images.
 * Paths that start with "res:" are bundled resources (seed photos); anything else is a file
 * path relative to the project folder (photos owners upload, receipts).
 */
public final class ImageUtil {

    private ImageUtil() {
    }

    public static final String RESOURCE_PREFIX = "res:";

    // DSA: HashMap cache - each photo is read from disk once, then found by its path in O(1).
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    /** Loads an image, or returns null if the file does not exist or cannot be read. */
    public static BufferedImage load(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }
        BufferedImage image = null;
        try {
            if (path.startsWith(RESOURCE_PREFIX)) {
                try (InputStream in = ImageUtil.class.getResourceAsStream("/" + path.substring(RESOURCE_PREFIX.length()))) {
                    if (in != null) {
                        image = ImageIO.read(in);
                    }
                }
            } else {
                File file = new File(path);
                if (file.isFile()) {
                    image = ImageIO.read(file);
                }
            }
        } catch (IOException e) {
            image = null;
        }
        if (image != null) {
            CACHE.put(path, image);
        }
        return image;
    }

    /** Scales and crops the image so it fills w x h completely (like CSS object-fit: cover). */
    public static BufferedImage cover(BufferedImage src, int w, int h, int arc) {
        BufferedImage out = new BufferedImage(Math.max(w, 1), Math.max(h, 1), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        quality(g);
        if (arc > 0) {
            g.setClip(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
        }
        if (src == null) {
            paintPlaceholder(g, w, h);
        } else {
            double scale = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
            int sw = (int) Math.ceil(src.getWidth() * scale);
            int sh = (int) Math.ceil(src.getHeight() * scale);
            g.drawImage(src, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
        }
        g.dispose();
        return out;
    }

    /**
     * Ready-to-use icon of a photo at a fixed size, with rounded corners. Missing photos get a placeholder.
     * It holds the photo at the common display scalings too, so Swing draws the sharp one instead of
     * stretching the 100% picture (which looked blurry at 125%).
     */
    public static ImageIcon photo(String path, int w, int h, int arc) {
        BufferedImage src = load(path);
        double[] scales = {1, 1.25, 1.5, 1.75, 2};
        java.awt.Image[] variants = new java.awt.Image[scales.length];
        for (int i = 0; i < scales.length; i++) {
            variants[i] = cover(src, px(w, scales[i]), px(h, scales[i]), px(arc, scales[i]));
        }
        return new ImageIcon(new java.awt.image.BaseMultiResolutionImage(variants));
    }

    // ------------------------------------------------------------------- sharp photos at any display scaling
    // Swing sizes are in "logical" pixels; at 125% display scaling each one covers 1.25 screen pixels.
    // A photo cut at the logical size is stretched by 1.25 when drawn, which blurs it. So photos are
    // cut at the real screen size (coverSharp) and drawn into the logical box (drawSharp), 1:1 on screen.

    /** How many screen pixels one Swing pixel covers where g draws (1.25 at 125% display scaling). */
    public static double deviceScale(Graphics2D g) {
        return Math.max(1, g.getTransform().getScaleX());
    }

    /** cover() at the real screen size for w × h logical pixels. Draw the result with drawSharp. */
    public static BufferedImage coverSharp(Graphics2D g, BufferedImage src, int w, int h, int arc) {
        double s = deviceScale(g);
        return cover(src, px(w, s), px(h, s), px(arc, s));
    }

    /** Draws an image from coverSharp into the w × h logical box at (0, 0). */
    public static void drawSharp(Graphics2D g, java.awt.Image image, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, w, h, null);
    }

    /** True when a cached coverSharp image no longer matches w × h logical pixels on g's screen. */
    public static boolean needsRedraw(BufferedImage cached, Graphics2D g, int w, int h) {
        double s = deviceScale(g);
        return cached == null || cached.getWidth() != px(w, s) || cached.getHeight() != px(h, s);
    }

    private static int px(int logical, double scale) {
        return Math.max(1, (int) Math.round(logical * scale));
    }

    /** Flat placeholder (a simple house shape) for properties without photos. */
    public static void paintPlaceholder(Graphics2D g, int w, int h) {
        quality(g);
        g.setColor(Colors.PRIMARY_TINT);
        g.fillRect(0, 0, w, h);
        int size = Math.min(w, h) / 3;
        int x = (w - size) / 2;
        int y = (h - size) / 2 + size / 6;
        g.setColor(Colors.LIGHT_BLUE);
        g.fillRect(x, y, size, size * 2 / 3);
        g.setColor(Colors.SECONDARY);
        g.fillPolygon(new int[]{x - size / 8, x + size / 2, x + size + size / 8},
                new int[]{y, y - size / 2, y}, 3);
        g.setColor(Colors.CARD);
        g.fillRect(x + size * 2 / 5, y + size / 4, size / 5, size * 5 / 12);
    }

    /** Folder (relative to the project folder) where photos owners pick are copied. */
    public static final String UPLOADS_DIR = "uploads/properties";

    /**
     * Copies a photo the owner picked into uploads/properties under a new unique name, so the
     * app keeps working if the original is moved or deleted. Returns the path to store.
     */
    public static String copyToUploads(File source) throws IOException {
        if (load(source.getPath()) == null) {
            throw new IOException(source.getName() + " is not a picture this app can open (use JPG or PNG).");
        }
        String name = source.getName();
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? ".png" : name.substring(dot).toLowerCase();
        File folder = new File(UPLOADS_DIR);
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("Could not create folder " + folder.getAbsolutePath());
        }
        File target = new File(folder, java.util.UUID.randomUUID() + extension);
        java.nio.file.Files.copy(source.toPath(), target.toPath());
        return UPLOADS_DIR + "/" + target.getName();
    }

    /** Draws a Swing component into a PNG file (receipts are saved this way). */
    public static void saveAsPng(JComponent component, File file) throws IOException {
        component.setSize(component.getPreferredSize());
        component.doLayout();
        layoutTree(component);
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        quality(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        component.printAll(g);
        g.dispose();
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create folder " + parent);
        }
        ImageIO.write(image, "png", file);
    }

    private static void layoutTree(java.awt.Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof java.awt.Container c) {
                layoutTree(c);
            }
        }
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}
