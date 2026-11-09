package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** Shows a receipt and lets the person save it as a PNG wherever they like. */
public final class ReceiptDialog {

    private ReceiptDialog() {
    }

    public static void show(Component parent, Receipt receipt, String suggestedFileName) {
        BufferedImage image = receipt.render();
        Image scaled = image.getScaledInstance(image.getWidth() / 2, image.getHeight() / 2, Image.SCALE_SMOOTH);
        JLabel picture = new JLabel(new ImageIcon(scaled));
        picture.setBorder(BorderFactory.createLineBorder(Colors.BORDER));

        AppDialog dialog = new AppDialog(parent, "Receipt");
        dialog.getBody().add(picture);
        AppButton save = new AppButton("Download receipt", AppButton.Style.SECONDARY).withIcon("download");
        save.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(suggestedFileName));
            chooser.setFileFilter(new FileNameExtensionFilter("PNG image", "png"));
            if (chooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new File(file.getParentFile(), file.getName() + ".png");
                }
                try {
                    receipt.save(file);
                    AppDialog.info(dialog, "Receipt saved to " + file.getAbsolutePath());
                } catch (IOException ex) {
                    AppDialog.error(dialog, "Could not save the receipt: " + ex.getMessage());
                }
            }
        });
        dialog.addButton(save);
        AppButton close = new AppButton("Close", AppButton.Style.PRIMARY);
        close.addActionListener(e -> dialog.dispose());
        dialog.addButton(close);
        dialog.showDialog(0);
    }
}
