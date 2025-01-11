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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public class DashboardPanel extends JPanel {

    private final JPanel contentPanel;
    private final double balance;
        private final String idCard;

    public DashboardPanel(JPanel contentPanel,double balance,String idCard) {
        this.contentPanel = contentPanel;
        this.balance = balance;
        this.idCard = idCard;
    }

    private String formatBalance(double balance) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        return numberFormat.format(balance); // Format with commas
    }
    private static String formatCurrency(double amount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedAmount = currencyFormat.format(amount);
        return formattedAmount.replace("₫", "") + " VNĐ";
    }
    
    public static int processCardExpirationAndBalance(String cardId) {
    String selectSQL = "SELECT card_id, ngay_het_han FROM card_info WHERE card_id = ?"; // Sửa câu lệnh SQL cho phù hợp
    int daysRemaining = -1; // Giá trị mặc định khi ngày hết hạn không hợp lệ
  
    try (
        Connection connection = DriverManager.getConnection(url, user, password);
        PreparedStatement balanceQuery = connection.prepareStatement(selectSQL)
    ) {
        // Thiết lập card_id vào câu lệnh SQL
        balanceQuery.setString(1, cardId);

        // Thực hiện truy vấn
        try (ResultSet resultSet = balanceQuery.executeQuery()) {
            if (resultSet.next()) {
                java.sql.Date ngayHetHan = resultSet.getDate("ngay_het_han");

                // Nếu ngày hết hạn không null
                if (ngayHetHan != null) {
                    // Chuyển đổi ngay_het_han từ java.sql.Date sang LocalDate
                    LocalDate expirationDate = ngayHetHan.toLocalDate();

                    // Lấy ngày hiện tại
                    LocalDate currentDate = LocalDate.now();

                    // Tính toán số ngày còn lại
                    daysRemaining = (int) ChronoUnit.DAYS.between(currentDate, expirationDate);

                    // Kiểm tra nếu ngày hết hạn đã qua
                    if (daysRemaining < 0) {
                        System.out.println("Ngày hết hạn đã qua.");
                        daysRemaining = -1; // Nếu ngày hết hạn đã qua, trả về -1
                    } else {
                        System.out.println("Số ngày còn lại: " + daysRemaining);
                    }
                } else {
                    System.err.println("Ngày hết hạn không hợp lệ.");
                }
            }
        }
    } catch (SQLException e) {
        System.err.println("Lỗi khi xử lý dữ liệu: " + e.getMessage());
    }

    return daysRemaining; // Trả về số ngày còn lại
}

     public Object[][] fetchBookData() {
        String selectSQL = "SELECT name, ngay_muon, price FROM SACH_MUON_TRA WHERE card_id = ?";
        ArrayList<Object[]> dataList = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {

            preparedStatement.setString(1, this.idCard);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    Date ngayMuon = resultSet.getDate("ngay_muon");
                     Double price = resultSet.getDouble("price");

                    dataList.add(new Object[]{name, ngayMuon != null ? ngayMuon.toString() : "", formatCurrency(price)});
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
        }
        return dataList.toArray(new Object[0][]);
    }

    @Override
    public void show() {
        // Create dashboard panel
        JPanel dashboardPanel = new JPanel(new GridLayout(1, 4, 30, 10));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 10));
        Object[][] bookDataList = fetchBookData();
        int borrowedBookCount = bookDataList.length;
        int borrowedDayCount = processCardExpirationAndBalance(idCard);
        System.out.println(borrowedDayCount);
        String balanceFormatted = formatBalance(balance);
        JLabel box1 = new JLabel("<html><div style='text-align: center;'>"
                + "<p style='vertical-align: middle;margin-bottom:4px'>"
                + "<img src='" + getClass().getResource("/image/Books-3d-icon.png") + "' width='50' height='50' /></p>"
                + "<span style='display: inline-block; vertical-align: middle;'>"
                + "Tổng số sách đang mượn<br>= " + borrowedBookCount + "</span></div></html>");
        JLabel box2 = new JLabel("<html><div style='text-align: center;'>"
                + "<p style='vertical-align: middle;margin-bottom:4px'>"
                + "<img src='" + getClass().getResource("/image/wallet-icon.png") + "' width='50' height='50' /></p>"
                + "<span style='display: inline-block; vertical-align: middle;'>"
                + "Số dư phí<br>" + balanceFormatted + " vnđ</span></div></html>");

        String statusText = "<html><div style='text-align: center;'>"
                + "<p style='vertical-align: middle;margin-bottom:4px'>"
                + "<img src='" + getClass().getResource("/image/Status-media-playlist-repeat-icon.png") + "' width='50' height='50' /></p>"
                + "<span style='display: inline-block; vertical-align: middle;'>"
                + "Trạng thái thẻ<br>";

        if (borrowedDayCount == -1) {
            statusText += "Hết hạn";
        } else {
            statusText += "Hạn<br>" + borrowedDayCount + " ngày";
        }
        statusText += "</span></div></html>";

        JLabel box3 = new JLabel(statusText);
        JLabel[] boxes = {box1, box2, box3};
        for (JLabel box : boxes) {
            box.setHorizontalAlignment(SwingConstants.CENTER);
            box.setVerticalAlignment(SwingConstants.CENTER);
            box.setFont(new Font("Arial", Font.BOLD, 20));
            box.setOpaque(true);
            box.setBackground(new Color(240, 240, 240));
            box.setBorder(BorderFactory.createRaisedSoftBevelBorder());
            dashboardPanel.add(box);
            box.setPreferredSize(new Dimension(230, 150));
        }

        // Tiêu đề
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel tableTitle = new JLabel("Danh sách mượn gần đây", SwingConstants.LEFT);
        tableTitle.setFont(new Font("Arial", Font.BOLD, 16));
        tableTitle.setOpaque(true);
        tableTitle.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));
        titlePanel.add(tableTitle);

        // Cột tiêu đề
        String[] columnNames = {"Tên sách", "Ngày mượn", "Giá"};

        // Lấy dữ liệu từ database
        Object[][] data = fetchBookData();

        // Tạo bảng
        JTable table = new JTable(data, columnNames);

        // Thiết lập hiển thị
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 36));
        header.setDefaultRenderer(new CustomHeaderRenderer());
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(36);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        // Cuộn bảng
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Cập nhật contentPanel
        contentPanel.removeAll();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(dashboardPanel);
        contentPanel.add(titlePanel);
        contentPanel.add(scrollPane);

        // Làm mới giao diện
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    static class CustomHeaderRenderer extends DefaultTableCellRenderer {

        public CustomHeaderRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER); // Căn giữa tiêu đề
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(new Font("Arial", Font.BOLD, 14)); // Đặt font chữ
            c.setForeground(Color.BLACK); // Màu chữ
            c.setBackground(new Color(240, 240, 240)); // Màu nền (Xanh steel blue)
            ((JLabel) c).setBorder(BorderFactory.createLineBorder(Color.BLACK, 1)); // Viền
            return c;
        }
    }
}
