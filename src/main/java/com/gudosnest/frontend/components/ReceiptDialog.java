package com.gudosnest.frontend.components;

import com.gudosnest.frontend.theme.Colors;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.io.IOException;

/**
 * Shows a receipt and lets the person save it as a PNG wherever they like. Opened from inside
 * another dialog (a booking, "Payment received"), it is that dialog's next page, with Back.
 */
public final class ReceiptDialog {

    private ReceiptDialog() {
    }

    public static void show(Component parent, Receipt receipt, String suggestedFileName) {
        JComponent picture = receipt.view();
        picture.setBorder(BorderFactory.createLineBorder(Colors.BORDER));

        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        if (owner instanceof AppDialog open && open.isShowing()) {
            open.nextPage("Receipt").add(picture, "growx 0, alignx center");
            open.addBackButton("Back");
            open.addButton(downloadButton(open, receipt, suggestedFileName));
            open.refit();
            return;
        }
        AppDialog dialog = new AppDialog(parent, "Receipt");
        dialog.getBody().add(picture, "growx 0, alignx center");
        dialog.addButton(downloadButton(dialog, receipt, suggestedFileName));
        dialog.showDialog(0);
    }

    private static AppButton downloadButton(AppDialog dialog, Receipt receipt, String suggestedFileName) {
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
                    AppDialog.info(dialog, "Receipt saved", "Saved to " + file.getAbsolutePath());
                } catch (IOException ex) {
                    AppDialog.error(dialog, "Couldn't save the receipt", ex.getMessage());
                }
            }
        });
        return save;
    }
}
