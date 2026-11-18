package com.gudosnest.frontend.owner;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.repository.ReportRepository.Query;
import com.gudosnest.frontend.components.AppButton;
import com.gudosnest.frontend.components.AppTable;
import com.gudosnest.frontend.components.Card;
import com.gudosnest.frontend.components.Formats;
import com.gudosnest.frontend.components.PageHeader;
import com.gudosnest.frontend.components.Pager;
import com.gudosnest.frontend.components.RowTableModel;
import com.gudosnest.frontend.components.Ui;
import com.gudosnest.frontend.portal.BasePage;
import com.gudosnest.frontend.portal.Portal;
import com.gudosnest.frontend.theme.Colors;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.Font;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The owner's reports: pick one of the eight questions and see the answer as a table. The SQL
 * behind it is hidden until "Show SQL" is clicked, and can be closed again. Read-only.
 */
public class ReportsPage extends BasePage {

    private int selected;
    private boolean sqlOpen;
    private final Pager pager = new Pager();

    public ReportsPage(Portal portal) {
        super(portal);
    }

    @Override
    protected void build() {
        List<Query> queries = portal.reports.queries();
        Query query = queries.get(selected);

        PageHeader header = new PageHeader("Reports");
        header.setSubtitle("Answers to common questions about your bookings, income and units.");
        JComboBox<String> picker = new JComboBox<>();
        for (Query q : queries) {
            picker.addItem(q.number() + ". " + q.question());
        }
        picker.setSelectedIndex(selected);
        picker.addActionListener(e -> {
            selected = picker.getSelectedIndex();
            pager.reset();
            reload();
        });
        header.addAction(picker);
        add(header, "gapbottom 24");

        Card info = new Card(new MigLayout("insets 16 20 12 20, fillx, wrap 1, gapy 0, hidemode 3", "[grow,fill]"));
        info.add(Ui.cardTitle(query.question()));

        JPanel sqlPanel = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 4", "[grow,fill]"));
        sqlPanel.setOpaque(false);
        sqlPanel.add(Ui.caption("SQL feature: " + query.feature()));
        JTextArea sql = new JTextArea(query.sql());
        sql.setEditable(false);
        sql.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        sql.setBackground(Colors.CARD);
        sql.setBorder(BorderFactory.createEmptyBorder());
        sqlPanel.add(sql, "growx, wmin 0");
        sqlPanel.setVisible(sqlOpen);

        AppButton toggle = new AppButton(sqlOpen ? "Hide SQL" : "Show SQL", AppButton.Style.TEXT);
        toggle.addActionListener(e -> {
            sqlOpen = !sqlOpen;
            sqlPanel.setVisible(sqlOpen);
            toggle.setText(sqlOpen ? "Hide SQL" : "Show SQL");
            info.revalidate();
            revalidate();
            repaint();
        });
        // The text button has an 8px left margin; remove it so its text lines up with the title
        toggle.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                toggle.getClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE) + ";margin:4,0,4,0");
        info.add(toggle, "alignx left, wmax pref, gaptop 4");
        info.add(sqlPanel, "growx, wmin 0, gaptop 8");
        add(info, "gapbottom 24");

        Db.Table result = portal.reports.run(query, portal.user().getId());
        if (result.rows().isEmpty()) {
            add(AppTable.emptyCard(query.empty()));
            return;
        }
        pager.setTotal(result.rows().size());
        pager.onChange(this::reload);
        DefaultTableModel model = new DefaultTableModel(result.columns().toArray(), 0);
        for (List<Object> row : pager.slice(result.rows())) {
            model.addRow(row.stream().map(ReportsPage::show).toArray());
        }
        JTable table = AppTable.create(model);
        table.setRowSelectionAllowed(false);
        RowTableModel.fitToRows(table);
        add(AppTable.inCard(table), "gapbottom 16");
        add(pager);
    }

    /** Readable text for a value: money, dates, and enum words without underscores. */
    private static String show(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal amount) {
            return Formats.money(amount);
        }
        if (value instanceof Timestamp t) {
            return Formats.dateTime(t.toLocalDateTime());
        }
        if (value instanceof LocalDateTime dt) {
            return Formats.dateTime(dt);
        }
        return value.toString().replace('_', ' ');
    }
}
