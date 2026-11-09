package sms;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Student Management System: a Swing desktop app for adding, updating,
 * deleting, searching and viewing student records.
 *
 * Usage: java -cp out sms.StudentManagementApp [path/to/students.csv]
 */
public class StudentManagementApp extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {"ID", "Name", "Age", "Email", "Phone", "Course"};
    private static final String[] COURSES = {
            "Computer Science", "Information Technology", "Electronics", "Mechanical Engineering",
            "Civil Engineering", "Business Administration", "Mathematics", "Physics"
    };
    private static final Color HEADER_BG = new Color(0x1F3A5F);
    private static final Color HEADER_SUB = new Color(0xC9D6E8);

    private final transient StudentRepository repo;

    private final JTextField idField = new JTextField(16);
    private final JTextField nameField = new JTextField(16);
    private final JTextField ageField = new JTextField(16);
    private final JTextField emailField = new JTextField(16);
    private final JTextField phoneField = new JTextField(16);
    private final JComboBox<String> courseBox = new JComboBox<>(COURSES);
    private final JTextField searchField = new JTextField();

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 2 ? Integer.class : String.class;
        }
    };
    private final JTable table = new JTable(tableModel);

    private final JButton addButton = new JButton("Add Student");
    private final JButton updateButton = new JButton("Update");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton clearButton = new JButton("Clear");

    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel countLabel = new JLabel(" ");

    /** ID of the student being edited, or null while the form is in "add new" mode. */
    private String selectedId;
    /** True while the code (not the user) is changing the table selection. */
    private boolean adjusting;

    public StudentManagementApp(StudentRepository repo) {
        super("Student Management System");
        this.repo = repo;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        courseBox.setEditable(true);
        wireActions();
        refreshTable();
        clearForm();

        setMinimumSize(new Dimension(920, 540));
        pack();
        setLocationRelativeTo(null);

        if (repo.getSkippedLineCount() > 0) {
            setStatus(repo.getSkippedLineCount() + " unreadable line(s) in the data file were ignored.");
        }
    }

    // ------------------------------------------------------------- Layout

    private JComponent buildHeader() {
        JLabel title = new JLabel("Student Management System");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel subtitle = new JLabel("Add, update, delete and view student records");
        subtitle.setForeground(HEADER_SUB);

        Box text = Box.createVerticalBox();
        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(subtitle);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        center.add(buildForm(), BorderLayout.WEST);
        center.add(buildTablePanel(), BorderLayout.CENTER);
        return center;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Student details"),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));

        int row = 0;
        row = addRow(form, row, "Student ID", idField);
        row = addRow(form, row, "Full name", nameField);
        row = addRow(form, row, "Age", ageField);
        row = addRow(form, row, "Email", emailField);
        row = addRow(form, row, "Phone", phoneField);
        row = addRow(form, row, "Course", courseBox);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);

        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0;
        bc.gridy = row++;
        bc.gridwidth = 2;
        bc.fill = GridBagConstraints.HORIZONTAL;
        bc.insets = new Insets(14, 4, 4, 4);
        form.add(buttons, bc);

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = row;
        filler.weighty = 1;
        form.add(Box.createGlue(), filler);
        return form;
    }

    private static int addRow(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints l = new GridBagConstraints();
        l.gridx = 0;
        l.gridy = row;
        l.anchor = GridBagConstraints.WEST;
        l.insets = new Insets(5, 4, 5, 10);
        panel.add(new JLabel(label), l);

        GridBagConstraints f = new GridBagConstraints();
        f.gridx = 1;
        f.gridy = row;
        f.weightx = 1;
        f.fill = GridBagConstraints.HORIZONTAL;
        f.insets = new Insets(5, 0, 5, 4);
        panel.add(field, f);
        return row + 1;
    }

    private JComponent buildTablePanel() {
        searchField.setToolTipText("Search by ID, name, age, email, phone or course");
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.add(new JLabel("Search:"), BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER);

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        int[] widths = {55, 140, 40, 200, 130, 170};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(735, 380));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(4, 14, 8, 14));
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(countLabel, BorderLayout.EAST);
        return bar;
    }

    // ------------------------------------------------------------ Wiring

    private void wireActions() {
        addButton.addActionListener(e -> addStudent());
        updateButton.addActionListener(e -> updateStudent());
        deleteButton.addActionListener(e -> deleteStudent());
        clearButton.addActionListener(e -> clearForm());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !adjusting) {
                onTableSelection();
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshTable();
            }
        });

        getRootPane().registerKeyboardAction(e -> clearForm(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ----------------------------------------------------------- Actions

    private void addStudent() {
        Student student = readValidatedStudent();
        if (student == null) {
            return;
        }
        try {
            repo.add(student);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            idField.requestFocusInWindow();
            return;
        } catch (IOException ex) {
            showError("Could not save the record: " + ex.getMessage());
            return;
        }
        searchField.setText("");
        clearForm();
        refreshTable();
        setStatus("Added " + student.getName() + " (" + student.getId() + ").");
    }

    private void updateStudent() {
        if (selectedId == null) {
            return;
        }
        Student student = readValidatedStudent();
        if (student == null) {
            return;
        }
        try {
            repo.update(student);
        } catch (NoSuchElementException ex) {
            showError(ex.getMessage());
            return;
        } catch (IOException ex) {
            showError("Could not save the record: " + ex.getMessage());
            return;
        }
        refreshTable();
        setStatus("Updated " + student.getName() + " (" + student.getId() + ").");
    }

    private void deleteStudent() {
        if (selectedId == null) {
            return;
        }
        Student student = repo.findById(selectedId).orElse(null);
        if (student == null) {
            clearForm();
            refreshTable();
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete " + student.getName() + " (" + student.getId() + ")?\nThis cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            repo.delete(student.getId());
        } catch (IOException ex) {
            showError("Could not save the change: " + ex.getMessage());
            return;
        }
        clearForm();
        refreshTable();
        setStatus("Deleted " + student.getName() + " (" + student.getId() + ").");
    }

    private void onTableSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        String id = (String) tableModel.getValueAt(table.convertRowIndexToModel(viewRow), 0);
        repo.findById(id).ifPresent(this::showStudent);
    }

    // ------------------------------------------------------ Form and table

    /** Returns the validated student, or null after telling the user what is wrong. */
    private Student readValidatedStudent() {
        String id = idField.getText();
        String name = nameField.getText();
        String age = ageField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String course = courseText();

        Map<String, String> errors = StudentValidator.validate(id, name, age, email, phone, course);
        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder("Please fix the following:\n");
            for (String error : errors.values()) {
                message.append("\n  • ").append(error);
            }
            JOptionPane.showMessageDialog(this, message.toString(), "Invalid input", JOptionPane.WARNING_MESSAGE);
            focusField(errors.keySet().iterator().next());
            return null;
        }
        return StudentValidator.build(id, name, age, email, phone, course);
    }

    private void focusField(String key) {
        JComponent target;
        switch (key) {
            case "id":
                target = idField;
                break;
            case "name":
                target = nameField;
                break;
            case "age":
                target = ageField;
                break;
            case "email":
                target = emailField;
                break;
            case "phone":
                target = phoneField;
                break;
            default:
                target = courseBox;
                break;
        }
        target.requestFocusInWindow();
    }

    private String courseText() {
        Object item = courseBox.getEditor().getItem();
        return item == null ? "" : item.toString();
    }

    private void showStudent(Student s) {
        selectedId = s.getId();
        idField.setText(s.getId());
        idField.setEditable(false);
        nameField.setText(s.getName());
        ageField.setText(String.valueOf(s.getAge()));
        emailField.setText(s.getEmail());
        phoneField.setText(s.getPhone());
        courseBox.setSelectedItem(s.getCourse());
        updateButtons();
    }

    private void clearForm() {
        adjusting = true;
        try {
            selectedId = null;
            idField.setText(repo.nextId());
            idField.setEditable(true);
            nameField.setText("");
            ageField.setText("");
            emailField.setText("");
            phoneField.setText("");
            courseBox.setSelectedItem("");
            table.clearSelection();
        } finally {
            adjusting = false;
        }
        updateButtons();
        nameField.requestFocusInWindow();
    }

    private void updateButtons() {
        boolean editing = selectedId != null;
        addButton.setEnabled(!editing);
        updateButton.setEnabled(editing);
        deleteButton.setEnabled(editing);
        getRootPane().setDefaultButton(editing ? updateButton : addButton);
    }

    private void refreshTable() {
        adjusting = true;
        try {
            // Forget the old selection, including JTable's remembered lead/anchor row, so the
            // row sorter never sees a stale index while the rows are rebuilt below.
            ListSelectionModel selection = table.getSelectionModel();
            selection.clearSelection();
            selection.setAnchorSelectionIndex(-1);
            selection.setLeadSelectionIndex(-1);

            tableModel.setRowCount(0);
            int shown = 0;
            for (Student s : repo.search(searchField.getText())) {
                tableModel.addRow(new Object[]{
                        s.getId(), s.getName(), s.getAge(), s.getEmail(), s.getPhone(), s.getCourse()});
                shown++;
            }
            if (selectedId != null) {
                selectRowById(selectedId);
            }
            countLabel.setText("Showing " + shown + " of " + repo.size() + " students");
        } finally {
            adjusting = false;
        }
    }

    private void selectRowById(String id) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (id.equalsIgnoreCase((String) tableModel.getValueAt(i, 0))) {
                int viewRow = table.convertRowIndexToView(i);
                table.setRowSelectionInterval(viewRow, viewRow);
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                return;
            }
        }
        table.clearSelection();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // --------------------------------------------------------------- Main

    public static void main(String[] args) {
        Path dataFile = Paths.get(args.length > 0 ? args[0] : "data/students.csv");
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
                // fall back to the default look and feel
            }
            try {
                new StudentManagementApp(new StudentRepository(dataFile)).setVisible(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not open the data file:\n" + dataFile.toAbsolutePath() + "\n\n" + ex.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
