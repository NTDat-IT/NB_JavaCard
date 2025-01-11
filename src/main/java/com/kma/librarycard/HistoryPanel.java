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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public class HistoryPanel extends JPanel {

    private final JPanel contentPanel;
    private final String idCard;

    public HistoryPanel(JPanel contentPanel, String idCard) {
        this.contentPanel = contentPanel;
        this.idCard = idCard;
    }

    /**
     * Fetches historical data from the database.
     *
     * @param action
     * @param result
     */
//    nam
    public static void insertRecord(String action, String result, String cardId) {
        String insertSQL = "INSERT INTO lich_su (date, action, result, card_id) VALUES (?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(url, user, password); 
             PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {

            // Get current time and format
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDate = now.format(formatter);

            // Set values for prepared statement
            preparedStatement.setString(1, formattedDate);
            preparedStatement.setString(2, action);
            preparedStatement.setString(3, result);
            preparedStatement.setString(4, cardId);

            // Execute the insertion
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm bản ghi vào lịch sử: " + e.getMessage());
            // You might want to re-throw a custom exception here for higher-level handling
        }
    }
//    nam



public static Object[][] fetchData(String cardId) {
    // Thêm ORDER BY date DESC để sắp xếp theo ngày giảm dần
    String selectSQL = "SELECT date, action, result FROM lich_su WHERE card_id = ? ORDER BY date DESC";
    ArrayList<Object[]> dataList = new ArrayList<>();

    try (Connection connection = DriverManager.getConnection(url, user, password);
         PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {

        preparedStatement.setString(1, cardId);

        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Timestamp timestamp = resultSet.getTimestamp("date"); // Lấy Timestamp thay vì Date
                String action = resultSet.getString("action");
                String result = resultSet.getString("result");

                // Chuyển đổi Timestamp thành LocalDateTime
                String formattedDate = "";
                if (timestamp != null) {
                    LocalDateTime localDateTime = timestamp.toLocalDateTime();
                    // Định dạng ngày, giờ, phút, giây
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss, dd/MM/yyyy");
                    formattedDate = localDateTime.format(formatter);
                }

                // Thêm dữ liệu vào danh sách
                dataList.add(new Object[]{
                    formattedDate,
                    action != null ? action : "",
                    result != null ? result : ""
                });
            }
        }

    } catch (SQLException e) {
        System.err.println("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
    }

    // Trả về mảng 2 chiều từ danh sách
    return dataList.toArray(new Object[0][]);
}



    /**
     * Displays the history panel with dynamically loaded data.
     */
    @Override
    public void show() {
        // Title Panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel("Lịch sử hoạt động");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanel.add(titleLabel);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 20));

        // Table for History
        String[] columnNames = {"Ngày", "Hành động", "Kết quả"};
        Object[][] data = fetchData(idCard);

        JTable historyTable = new JTable(data, columnNames);
        JTableHeader tableHeader = historyTable.getTableHeader();
        tableHeader.setPreferredSize(new Dimension(0, 36));
        tableHeader.setFont(new Font("Arial", Font.BOLD, 14));
        tableHeader.setDefaultRenderer(new CustomHeaderRenderer());

        // Center align the table cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < historyTable.getColumnCount(); i++) {
            historyTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        historyTable.setFont(new Font("Arial", Font.PLAIN, 14));
        historyTable.setRowHeight(36);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // Main Content Panel
        contentPanel.removeAll();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(titlePanel);
        contentPanel.add(scrollPane);
        contentPanel.add(buttonPanel);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Custom header renderer for table headers.
     */
    static class CustomHeaderRenderer extends DefaultTableCellRenderer {

        public CustomHeaderRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(new Font("Arial", Font.BOLD, 14));
            c.setForeground(Color.BLACK);
            c.setBackground(new Color(240, 240, 240));
            ((JLabel) c).setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            return c;
        }
    }
}