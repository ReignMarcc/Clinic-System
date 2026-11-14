package com.clinic.ui;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Action column of the Payments table:
 *   unpaid bill            -> Pay
 *   paid bill              -> Receipt, plus Complete while the visit is still open
 *   cancelled appointment  -> "-"
 */
final class BillActionCell extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

    private final Supplier<List<BillRow>> rows;
    private final Consumer<BillRow> onPay;
    private final Consumer<BillRow> onReceipt;
    private final Consumer<BillRow> onComplete;

    BillActionCell(Supplier<List<BillRow>> rows, Consumer<BillRow> onPay,
                   Consumer<BillRow> onReceipt, Consumer<BillRow> onComplete) {
        this.rows = rows;
        this.onPay = onPay;
        this.onReceipt = onReceipt;
        this.onComplete = onComplete;
    }

    private JComponent build(int row, boolean live) {
        JPanel holder = UiKit.actionRow(14);
        List<BillRow> list = rows.get();
        if (row < 0 || row >= list.size()) return holder;
        BillRow r = list.get(row);

        if (r.isCancelled()) {
            JLabel dash = new JLabel("-");
            dash.setForeground(Theme.TEXT_MUTED);
            holder.add(dash);
            return holder;
        }

        if (r.isPaid()) {
            JButton receipt = button("Receipt", Theme.PRIMARY, Color.WHITE, Theme.PRIMARY, 72);
            if (live) receipt.addActionListener(e -> { fireEditingStopped(); onReceipt.accept(r); });
            holder.add(receipt);
            if (r.isVisitOpen()) {
                JButton complete = button("Complete", Color.WHITE, Theme.DONE_FG, null, 84);
                if (live) complete.addActionListener(e -> { fireEditingStopped(); onComplete.accept(r); });
                holder.add(Box.createHorizontalStrut(6));
                holder.add(complete);
            }
            return holder;
        }

        JButton pay = button("Pay", Color.WHITE, Theme.PRIMARY, Theme.PRIMARY, 72);
        if (live) pay.addActionListener(e -> { fireEditingStopped(); onPay.accept(r); });
        holder.add(pay);
        return holder;
    }

    private static JButton button(String text, Color fg, Color bg, Color outline, int width) {
        JButton b = UiKit.tableActionButton(text, fg, bg, outline);
        b.setPreferredSize(new Dimension(width, 32));
        return b;
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
        return build(r, false);
    }

    @Override
    public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
        return build(r, true);
    }

    @Override public Object getCellEditorValue() { return ""; }
}