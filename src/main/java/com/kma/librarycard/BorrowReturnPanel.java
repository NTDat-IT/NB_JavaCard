package com.kma.librarycard;

import static com.kma.librarycard.MainPage.password;
import static com.kma.librarycard.MainPage.url;
import static com.kma.librarycard.MainPage.user;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class BorrowReturnPanel extends JPanel {

    private final JPanel contentPanel;
    private JTable mainTableBorrow;
    private final ProfilePanel profilePanel;
    private TableRowSorter<DefaultTableModel> sorterBorrow; // Thêm sorter cho bảng chính

    public BorrowReturnPanel(JPanel contentPanel, ProfilePanel profilePanel) {
        this.contentPanel = contentPanel;
        this.profilePanel = profilePanel;
    }

    private static String formatCurrency(double amount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedAmount = currencyFormat.format(amount);
        return formattedAmount.replace("₫", "") + " VNĐ";
    }

    public static Object[][] fetchData(boolean isBorrowTable, ProfilePanel profilePanel) {
        String cardID = (profilePanel != null) ? profilePanel.card_id : null;
        String selectSQL = isBorrowTable
                ? "SELECT name, ngay_muon, ngay_het_han, status, price FROM SACH_MUON_TRA WHERE ngay_tra IS NULL AND card_id = ?"
                : "SELECT id_sach, name FROM SACH";

        ArrayList<Object[]> dataList = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {

            if (isBorrowTable && cardID != null) {
                preparedStatement.setString(1, cardID);
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    if (isBorrowTable) {
                        String name = resultSet.getString("name");
                        Date ngayMuon = resultSet.getDate("ngay_muon");
                        Date ngayTra = resultSet.getDate("ngay_het_han");
                        String status = resultSet.getString("status");
                        Double price = resultSet.getDouble("price");
                        dataList.add(new Object[]{name, formatCurrency(price), ngayMuon != null ? ngayMuon.toString() : "",
                                ngayTra != null ? ngayTra.toString() : "", status});

                    } else {
                        int idSach = resultSet.getInt("id_sach");
                        String name = resultSet.getString("name");
                        dataList.add(new Object[]{idSach, name});
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
            e.printStackTrace();

        }
        return dataList.toArray(Object[][]::new);
    }

    public static Object[][] fetchDataBorrowed(boolean isBorrowTable, ProfilePanel profilePanel) {
        String cardID = (profilePanel != null) ? profilePanel.card_id : null;
        String selectSQL = isBorrowTable && cardID != null
                ? "SELECT b.name, b.price, b.quantity FROM book b LEFT JOIN sach_muon_tra s ON b.name = s.name WHERE (s.name IS NULL OR s.card_id != ?) AND b.quantity > 0"
                : "SELECT name, price, quantity FROM book WHERE quantity > 0";
        ArrayList<Object[]> dataList = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {

            if (isBorrowTable && cardID != null) {
                preparedStatement.setString(1, cardID);

            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    double price = resultSet.getDouble("price");
                    int quantity = resultSet.getInt("quantity");
                    dataList.add(new Object[]{name, formatCurrency(price), quantity});
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
            e.printStackTrace();

        }
        return dataList.toArray(Object[][]::new);
    }

    @Override
    public void show() {
        JPanel titlePanelBorrow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanelBorrow.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 20));
        JLabel tableTitleBorrow = new JLabel("Danh sách đang mượn");
        tableTitleBorrow.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanelBorrow.add(tableTitleBorrow);

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); // Tạo searchPanel
        JLabel searchLabel = new JLabel("Tìm kiếm:");
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField searchField = new JTextField(20); // Tạo searchField
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(180, 30));
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);


        String[] columnNamesBorrow = {"Tên sách", "Giá", "Ngày mượn", "Ngày hết hạn", "Trạng thái"};
        Object[][] dataBorrow = fetchData(true, profilePanel);

        DefaultTableModel modelBorrow = new DefaultTableModel(dataBorrow, columnNamesBorrow);
        sorterBorrow = new TableRowSorter<>(modelBorrow); // Khởi tạo sorter
        mainTableBorrow = new JTable(modelBorrow);
        mainTableBorrow.setRowSorter(sorterBorrow); // Set sorter cho bảng


        JTableHeader headerBorrow = mainTableBorrow.getTableHeader();
        headerBorrow.setPreferredSize(new Dimension(0, 36));
        headerBorrow.setFont(new Font("Arial", Font.BOLD, 14));
        headerBorrow.setDefaultRenderer(new CustomHeaderRenderer());

        DefaultTableCellRenderer centerRendererBorrow = new DefaultTableCellRenderer();
        centerRendererBorrow.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < mainTableBorrow.getColumnCount(); i++) {
            mainTableBorrow.getColumnModel().getColumn(i).setCellRenderer(centerRendererBorrow);
        }
        mainTableBorrow.setFont(new Font("Arial", Font.PLAIN, 14));
        mainTableBorrow.setRowHeight(36);

        JScrollPane scrollPaneBorrow = new JScrollPane(mainTableBorrow);
        scrollPaneBorrow.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Thêm KeyListener cho searchField để tìm kiếm
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = searchField.getText();
                if (text.trim().length() == 0) {
                    sorterBorrow.setRowFilter(null);
                } else {
                    sorterBorrow.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        JPanel buttonPanelBorrow = new JPanel();
        buttonPanelBorrow.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton buttonEdit = new JButton("Mượn sách");
        JButton buttonExtend = new JButton("Trả sách");

        buttonEdit.setPreferredSize(new Dimension(150, 40));
        buttonEdit.setBackground(new Color(0, 28, 68));
        buttonExtend.setPreferredSize(new Dimension(150, 40));
        buttonExtend.setBackground(new Color(0, 28, 68));
        buttonEdit.setForeground(Color.WHITE);
        buttonExtend.setForeground(Color.WHITE);

        buttonEdit.addActionListener(e -> showBookSelectionDialog(true));
        buttonExtend.addActionListener(e -> showReturnBookSelectionDialog());
        buttonPanelBorrow.add(buttonEdit);
        buttonPanelBorrow.add(buttonExtend);

        contentPanel.removeAll();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(titlePanelBorrow);
        contentPanel.add(searchPanel); // Thêm search panel
        contentPanel.add(scrollPaneBorrow);
        contentPanel.add(buttonPanelBorrow);

        contentPanel.revalidate();
        contentPanel.repaint();
    }


    private void showBookSelectionDialog(boolean isBorrow) {
        JDialog bookDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this),
                isBorrow ? "Chọn Sách Mượn" : "Chọn Sách Trả", true);
        bookDialog.setLayout(new BorderLayout());

         // Search Panel cho dialog
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel searchLabel = new JLabel("Tìm kiếm:");
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(180, 30));
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);


        String[] columnNames = isBorrow ? new String[]{"Chọn", "Tên Sách", "Giá", "Số lượng"} : new String[]{"Chọn", "Tên Sách", "Giá"};
        Object[][] data = fetchDataBorrowed(true, profilePanel);


        Object[][] tableData = new Object[data.length][4];
        for (int i = 0; i < data.length; i++) {
            tableData[i][0] = Boolean.FALSE;
            tableData[i][1] = data[i][0];
            tableData[i][2] = data[i][1];
            tableData[i][3] = data[i][2];
        }

        DefaultTableModel model = new DefaultTableModel(tableData, columnNames) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                }
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
          TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model); // Tạo sorter cho bảng trong dialog
        JTable bookTable = new JTable(model);
         bookTable.setRowSorter(sorter);  // Set sorter cho bảng trong dialog
        JTableHeader headerBook = bookTable.getTableHeader();
        headerBook.setPreferredSize(new Dimension(0, 36));
        headerBook.setFont(new Font("Arial", Font.BOLD, 14));
        headerBook.setDefaultRenderer(new CustomHeaderRenderer());

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i < bookTable.getColumnCount(); i++) {
            bookTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        bookTable.setFont(new Font("Arial", Font.PLAIN, 14));
        bookTable.setRowHeight(36);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        JScrollPane scrollPane = new JScrollPane(bookTable);

         // Thêm KeyListener cho searchField trong dialog
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                 String text = searchField.getText();
                if (text.trim().length() == 0) {
                     sorter.setRowFilter(null);
                } else {
                  sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
               }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton selectButton = new JButton("Chọn");
        JButton cancelButton = new JButton("Hủy");
        selectButton.setPreferredSize(new Dimension(100, 35));
        selectButton.setBackground(new Color(0, 28, 68));
        selectButton.setForeground(Color.WHITE);
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.setBackground(new Color(0, 28, 68));
        cancelButton.setForeground(Color.WHITE);

        selectButton.addActionListener(e -> {
            List<String> selectedBooks = new ArrayList<>();
            for (int i = 0; i < bookTable.getRowCount(); i++) {
                Boolean isChecked = (Boolean) bookTable.getValueAt(i, 0);
                if (isChecked) {
                    String selectedBook = bookTable.getValueAt(i, 1).toString();
                    selectedBooks.add(selectedBook);
                }
            }
            if (selectedBooks.isEmpty()) {
                JOptionPane.showMessageDialog(bookDialog, "Vui lòng chọn ít nhất một sách.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                if (isBorrow) {
                    borrowBooks(selectedBooks);
                    HistoryPanel.insertRecord("Mượn sách", "Thành công", profilePanel.card_id);
                } else {
                    HistoryPanel.insertRecord("Mượn sách", "Thất bại", profilePanel.card_id);
                }
                bookDialog.dispose();
            }
        });
        cancelButton.addActionListener(e -> bookDialog.dispose());
        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);


        bookDialog.add(searchPanel, BorderLayout.NORTH); // Thêm search panel vào dialog
        bookDialog.add(scrollPane, BorderLayout.CENTER);
        bookDialog.add(buttonPanel, BorderLayout.SOUTH);
        bookDialog.setSize(600, 400);
        bookDialog.setLocationRelativeTo(this);
        bookDialog.setVisible(true);
    }


    private void showReturnBookSelectionDialog() {
        JDialog bookDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Chọn Sách Trả", true);
        bookDialog.setLayout(new BorderLayout());

        // Search Panel cho dialog
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel searchLabel = new JLabel("Tìm kiếm:");
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(180, 30));
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        String[] columnNames = new String[]{"Chọn", "Tên Sách", "Giá"};
        Object[][] data = fetchData(true, profilePanel);


        Object[][] tableData = new Object[data.length][3];
        for (int i = 0; i < data.length; i++) {
            tableData[i][0] = Boolean.FALSE;
            tableData[i][1] = data[i][0];
            tableData[i][2] = data[i][1];
        }

        DefaultTableModel model = new DefaultTableModel(tableData, columnNames) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                }
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
          TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);  // Tạo sorter cho bảng trong dialog
        JTable bookTable = new JTable(model);
        bookTable.setRowSorter(sorter);  // Set sorter cho bảng trong dialog

        JTableHeader headerBook = bookTable.getTableHeader();
        headerBook.setPreferredSize(new Dimension(0, 36));
        headerBook.setFont(new Font("Arial", Font.BOLD, 14));
        headerBook.setDefaultRenderer(new CustomHeaderRenderer());

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i < bookTable.getColumnCount(); i++) {
            bookTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        bookTable.setFont(new Font("Arial", Font.PLAIN, 14));
        bookTable.setRowHeight(36);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        JScrollPane scrollPane = new JScrollPane(bookTable);

        // Thêm KeyListener cho searchField trong dialog
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                 String text = searchField.getText();
                 if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton selectButton = new JButton("Chọn");
        JButton cancelButton = new JButton("Hủy");
        selectButton.setPreferredSize(new Dimension(100, 35));
        selectButton.setBackground(new Color(0, 28, 68));
        selectButton.setForeground(Color.WHITE);
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.setBackground(new Color(0, 28, 68));
        cancelButton.setForeground(Color.WHITE);

        selectButton.addActionListener(e -> {
            List<String> selectedBooks = new ArrayList<>();
            for (int i = 0; i < bookTable.getRowCount(); i++) {
                Boolean isChecked = (Boolean) bookTable.getValueAt(i, 0);
                if (isChecked) {
                    String selectedBook = bookTable.getValueAt(i, 1).toString();
                    selectedBooks.add(selectedBook);
                }
            }
            if (selectedBooks.isEmpty()) {
                JOptionPane.showMessageDialog(bookDialog, "Vui lòng chọn ít nhất một sách.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                returnBooks(selectedBooks);
                HistoryPanel.insertRecord("Trả sách", "Thành công", profilePanel.card_id);
                bookDialog.dispose();
            }
        });
        cancelButton.addActionListener(e -> bookDialog.dispose());
        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);

        bookDialog.add(searchPanel, BorderLayout.NORTH); // Thêm search panel vào dialog
        bookDialog.add(scrollPane, BorderLayout.CENTER);
        bookDialog.add(buttonPanel, BorderLayout.SOUTH);
        bookDialog.setSize(600, 400);
        bookDialog.setLocationRelativeTo(this);
        bookDialog.setVisible(true);
    }


    private void borrowBooks(List<String> selectedBooks) {
        String insertSQL = "INSERT INTO SACH_MUON_TRA (name, price, ngay_muon, ngay_het_han, status, card_id) VALUES (?, ?, ?, ?, ?, ?)";
        String updateQuantitySQL = "UPDATE book SET quantity = quantity - 1 WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
             PreparedStatement updateStatement = connection.prepareStatement(updateQuantitySQL)) {

            for (String selectedBook : selectedBooks) {
                Double price = 0.0;
                String getPriceSQL = "SELECT price FROM book WHERE name = ?";
                try (PreparedStatement priceStatement = connection.prepareStatement(getPriceSQL)) {
                    priceStatement.setString(1, selectedBook);
                    ResultSet priceResult = priceStatement.executeQuery();
                    if (priceResult.next()) {
                        price = priceResult.getDouble("price");
                    }
                }
                insertStatement.setString(1, selectedBook);
                insertStatement.setDouble(2, price);
                insertStatement.setDate(3, new java.sql.Date(new Date().getTime()));
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.add(java.util.Calendar.MONTH, 1);
                insertStatement.setDate(4, new java.sql.Date(calendar.getTimeInMillis()));
                insertStatement.setString(5, "Chưa thanh toán");
                insertStatement.setString(6, profilePanel.card_id);
                insertStatement.executeUpdate();

                updateStatement.setString(1, selectedBook);
                updateStatement.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Mượn sách thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            updateMainTable();
        } catch (SQLException ex) {
            System.err.println("Lỗi khi mượn sách: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi khi mượn sách: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void returnBooks(List<String> selectedBooks) {
        String deleteSQL = "DELETE FROM SACH_MUON_TRA WHERE name = ? AND ngay_tra IS NULL AND card_id = ?";
        String updateQuantitySQL = "UPDATE book SET quantity = quantity + 1 WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement deleteStatement = connection.prepareStatement(deleteSQL);
             PreparedStatement updateStatement = connection.prepareStatement(updateQuantitySQL)) {

            for (String selectedBook : selectedBooks) {
                deleteStatement.setString(1, selectedBook);
                deleteStatement.setString(2, profilePanel.card_id);
                int rowsAffected = deleteStatement.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Đã xóa " + rowsAffected + " bản ghi cho sách " + selectedBook);

                    updateStatement.setString(1, selectedBook);
                    updateStatement.executeUpdate();
                } else {
                    System.err.println("Không tìm thấy bản ghi cần xóa cho sách: " + selectedBook);
                }
            }
            JOptionPane.showMessageDialog(this, "Trả sách thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            updateMainTable();
        } catch (SQLException ex) {
            System.err.println("Lỗi khi trả sách: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi trả sách: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void updateMainTable() {
        Object[][] newData = fetchData(true, profilePanel);
        DefaultTableModel model = new DefaultTableModel(newData, new String[]{"Tên sách", "Giá", "Ngày mượn", "Ngày hết hạn", "Trạng thái"});
        mainTableBorrow.setModel(model);
        sorterBorrow.setModel(model); // Cập nhật sorter
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < mainTableBorrow.getColumnCount(); i++) {
            mainTableBorrow.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        mainTableBorrow.setFont(new Font("Arial", Font.PLAIN, 14));
        mainTableBorrow.setRowHeight(36);
        mainTableBorrow.revalidate();
        mainTableBorrow.repaint();

    }


    static class CustomHeaderRenderer extends DefaultTableCellRenderer {

        public CustomHeaderRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(new Font("Arial", Font.BOLD, 14));
            c.setForeground(Color.BLACK);
            c.setBackground(new Color(240, 240, 240));
            ((JLabel) c).setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            return c;
        }
    }
}