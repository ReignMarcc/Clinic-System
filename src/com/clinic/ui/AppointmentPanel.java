package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.service.ClinicManager;
import javax.swing.border.CompoundBorder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * "Pending Appointments & Reports" page.
 *
 * - Two stat tiles: Active Patients, Pending Appointments.
 * - Generate Report just opens the Reports page for now (no other action yet).
 * - Each row has a View button that offers "Edit Records" / "Cancel Appointment",
 *   both of which open that patient's record (the actual edit/cancel happens there).
 */
public class AppointmentPanel extends JPanel {

 private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

 private final ClinicManager clinic;
 private final Runnable onAddPatient;
 private final Runnable onGenerateReport;
 private final Consumer<Patient> onViewPatient;

 private final UiKit.Card patientsTile = statTile(Icons.Type.PEOPLE, Theme.TILE_BLUE);
 private final UiKit.Card pendingTile = statTile(Icons.Type.CALENDAR_CLOCK, Theme.TILE_YELLOW);
 private final JLabel patientsValue = tileValue();
 private final JLabel pendingValue = tileValue();

 private final DefaultTableModel model = new DefaultTableModel(
         new String[]{"Patient", "Doctor", "Date", "Status", "Action"}, 0) {
  @Override public boolean isCellEditable(int r, int c) { return c == 4; }
 };
 private final JTable table = new JTable(model);
 private final JLabel footNote = new JLabel(" ");
 private List<Appointment> rows;

 public AppointmentPanel(ClinicManager clinic, Runnable onAddPatient, Runnable onGenerateReport,
                         Consumer<Patient> onViewPatient) {
  this.clinic = clinic;
  this.onAddPatient = onAddPatient;
  this.onGenerateReport = onGenerateReport;
  this.onViewPatient = onViewPatient;

  setLayout(new BorderLayout(0, 18));
  setBackground(Theme.CONTENT_BG);
  setBorder(new EmptyBorder(18, 22, 22, 22));

  add(UiKit.pageTopBar("Pending Appointments / Active Patients"), BorderLayout.NORTH);

  JPanel body = new JPanel(new BorderLayout(0, 18));
  body.setOpaque(false);
  body.add(buildStatsRow(), BorderLayout.NORTH);
  body.add(buildTableCard(), BorderLayout.CENTER);
  add(body, BorderLayout.CENTER);

  refresh();
 }

 // ------------------------------------------------------------------
 // Data
 // ------------------------------------------------------------------

 public void refresh() {
  patientsValue.setText(String.valueOf(clinic.getPatients().size()));
  pendingValue.setText(String.valueOf(clinic.getPendingAppointments().size()));

  Map<String, String> names = new HashMap<>();
  for (Patient p : clinic.getPatients()) names.put(p.getPatientId().toUpperCase(), p.getName());
  Map<String, String> docNames = new HashMap<>();
  for (Doctor d : clinic.getDoctors()) docNames.put(d.getDoctorId().toUpperCase(), d.getName());

  rows = new java.util.ArrayList<>(clinic.getAppointments());
  rows.sort(java.util.Comparator.comparing(Appointment::getDateTime));

  model.setRowCount(0);
  for (Appointment a : rows) {
   model.addRow(new Object[]{
           names.getOrDefault(a.getPatientId().toUpperCase(), a.getPatientId()),
           a.getDoctorId().isEmpty() ? "Unassigned" : docNames.getOrDefault(a.getDoctorId().toUpperCase(), "-"),
           a.getDateTime().format(DATE_FMT),
           a.getStatus().name(),
           "View"});
  }
  footNote.setText(clinic.getPatients().size() + " active patients  -  "
          + clinic.getPendingAppointments().size() + " pending appointments");
 }

 // ------------------------------------------------------------------
 // Layout
 // ------------------------------------------------------------------

 private JComponent buildStatsRow() {
  JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
  row.setOpaque(false);

  fillTile(patientsTile, patientsValue, "Active Patient's");
  fillTile(pendingTile, pendingValue, "Pending Appointments");

  PillButton generate = PillButton.primary("Generate Report", Icons.of(Icons.Type.REPORTS, 22, Color.WHITE));
  generate.setFont(Theme.font(Font.BOLD, 16));
  generate.setPreferredSize(new Dimension(220, 150));
  generate.addActionListener(e -> onGenerateReport.run());

  row.add(patientsTile);
  row.add(pendingTile);
  row.add(wrapRight(generate));
  return row;
 }

 private static JComponent wrapRight(JComponent c) {
  JPanel p = new JPanel(new GridBagLayout());
  p.setOpaque(false);
  GridBagConstraints gc = new GridBagConstraints();
  gc.anchor = GridBagConstraints.NORTH;
  p.add(c, gc);
  return p;
 }

 private static UiKit.Card statTile(Icons.Type icon, Color tileColor) {
  UiKit.Card card = new UiKit.Card(18, Color.WHITE, Theme.CARD_STROKE);
  card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
  card.setBorder(new EmptyBorder(16, 18, 14, 18));
  card.setPreferredSize(new Dimension(230, 150));
  return card;
 }

 private static JLabel tileValue() {
  JLabel l = new JLabel("0");
  l.setFont(Theme.font(Font.BOLD, 34));
  l.setForeground(Theme.TEXT_DARK);
  l.setAlignmentX(Component.LEFT_ALIGNMENT);
  return l;
 }

 private void fillTile(UiKit.Card tile, JLabel value, String caption) {
  if (tile.getComponentCount() > 0) return; // only build once
  Icons.Type icon = tile == patientsTile ? Icons.Type.PEOPLE : Icons.Type.CALENDAR_CLOCK;
  Color color = tile == patientsTile ? Theme.TILE_BLUE : Theme.TILE_YELLOW;

  JComponent iconTile = new JComponent() {
   @Override
   protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    Theme.antialias(g2);
    g2.setColor(color);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
    Icons.of(icon, 26, Theme.TEXT_DARK).paintIcon(this, g2, (getWidth() - 26) / 2, (getHeight() - 26) / 2);
    g2.dispose();
   }
  };
  Dimension d = new Dimension(46, 46);
  iconTile.setPreferredSize(d);
  iconTile.setMaximumSize(d);
  iconTile.setAlignmentX(Component.LEFT_ALIGNMENT);

  JLabel cap = new JLabel(caption);
  cap.setFont(Theme.font(Font.PLAIN, 15));
  cap.setForeground(Theme.TEXT_DARK);
  cap.setAlignmentX(Component.LEFT_ALIGNMENT);

  tile.add(iconTile);
  tile.add(Box.createVerticalStrut(10));
  tile.add(value);
  tile.add(cap);
 }

 private JComponent buildTableCard() {
  UiKit.Card card = new UiKit.Card(18, Color.WHITE, null);
  card.setLayout(new BorderLayout(0, 12));
  card.setBorder(new EmptyBorder(18, 24, 16, 24));

  JLabel title = new JLabel("Pending Appointments");
  title.setFont(Theme.font(Font.BOLD, 22));
  title.setForeground(Theme.TEXT_DARK);
  card.add(title, BorderLayout.NORTH);

  UiKit.styleTable(table);
  table.setRowHeight(52);
  table.getColumnModel().getColumn(0).setPreferredWidth(220);
  table.getColumnModel().getColumn(1).setPreferredWidth(140);
  table.getColumnModel().getColumn(2).setPreferredWidth(90);
  table.getColumnModel().getColumn(3).setPreferredWidth(130);
  table.getColumnModel().getColumn(4).setPreferredWidth(90);
  table.getColumnModel().getColumn(3).setCellRenderer(new UiKit.StatusRenderer());

  ViewButtonCell viewCell = new ViewButtonCell();
  table.getColumnModel().getColumn(4).setCellRenderer(viewCell);
  table.getColumnModel().getColumn(4).setCellEditor(viewCell);

  JScrollPane scroll = new JScrollPane(table);
  scroll.setBorder(null);
  scroll.getViewport().setBackground(Color.WHITE);
  card.add(scroll, BorderLayout.CENTER);

  footNote.setFont(Theme.font(Font.PLAIN, 13));
  footNote.setForeground(Theme.TEXT_MUTED);
  card.add(footNote, BorderLayout.SOUTH);
  return card;
 }

 // ------------------------------------------------------------------
 // View button -> popup with Edit Records / Cancel Appointment
 // ------------------------------------------------------------------

 private void showViewMenu(int row) {
  if (rows == null || row < 0 || row >= rows.size()) return;
  Appointment a = rows.get(row);
  Patient patient = findPatient(a.getPatientId());
  if (patient == null) return;

  JPopupMenu menu = new JPopupMenu();
  menu.setBackground(Color.WHITE);
  menu.setBorder(new CompoundBorder(new LineBorder(Theme.CARD_STROKE), new EmptyBorder(6, 6, 6, 6)));

  AccountMenu.MenuButton edit = new AccountMenu.MenuButton("Edit Records", Theme.TEXT_DARK);
  edit.addActionListener(e -> { menu.setVisible(false); onViewPatient.accept(patient); });
  AccountMenu.MenuButton cancel = new AccountMenu.MenuButton("Cancel Appointment", Theme.ERROR);
  cancel.addActionListener(e -> { menu.setVisible(false); onViewPatient.accept(patient); });
  menu.add(edit);
  menu.add(cancel);

  // lalabas sa ilalim ng View button ng row na pinindot, nakadikit sa kanang gilid
  Rectangle cell = table.getCellRect(row, 4, true);
  int x = cell.x + cell.width - menu.getPreferredSize().width - 8;
  int y = cell.y + cell.height - 6;
  menu.show(table, Math.max(0, x), y);
 }

 private Patient findPatient(String patientId) {
  for (Patient p : clinic.getPatients()) {
   if (p.getPatientId().equalsIgnoreCase(patientId)) return p;
  }
  return null;
 }

 /** Renders and edits the Action column as a small "View" button. */
 private class ViewButtonCell extends AbstractCellEditor implements javax.swing.table.TableCellRenderer, javax.swing.table.TableCellEditor {
  private int editingRow;

  private JButton makeButton() {
   JButton b = UiKit.tableActionButton("View", Theme.PRIMARY, Color.WHITE, Theme.PRIMARY);
   b.setPreferredSize(new Dimension(70, 32));
   return b;
  }

  @Override
  public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
   JPanel holder = new JPanel(new GridBagLayout());
   holder.setOpaque(true);
   holder.setBackground(Color.WHITE);
   holder.add(makeButton());
   return holder;
  }

  @Override
  public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
   editingRow = r;
   JPanel holder = new JPanel(new GridBagLayout());
   holder.setOpaque(true);
   holder.setBackground(Color.WHITE);
   JButton b = makeButton();
   b.addActionListener(e -> {
    fireEditingStopped();
    showViewMenu(editingRow);
   });
   holder.add(b);
   return holder;
  }

  @Override public Object getCellEditorValue() { return "View"; }
 }
}