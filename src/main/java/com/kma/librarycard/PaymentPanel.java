package com.kma.librarycard;

import static com.kma.librarycard.MainPage.password;
import static com.kma.librarycard.MainPage.url;
import static com.kma.librarycard.MainPage.user;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

public class PaymentPanel extends JPanel {

    private final JPanel contentPanel;
    private JTable tablePayment;
    private DefaultTableModel modelPayment;
    private final ProfilePanel profilePanel;
    private final double balance;
    public String card_id = "";

    public PaymentPanel(JPanel contentPanel, ProfilePanel profilePanel, double balance) {
        this.contentPanel = contentPanel;
        this.profilePanel = profilePanel;
        this.balance = balance;
        try {
            byte[] cardIdBytes = getCardID();
            if (cardIdBytes != null) {
                this.card_id = new String(cardIdBytes);
            } else {
                this.card_id = "N/A";
            }

        } catch (CardException ex) {
            this.card_id = "N/A";
            Logger.getLogger(ProfilePanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static List<Object[]> fetchDataList(String cardID) {
        String selectSQL = "SELECT * FROM sach_muon_tra WHERE ngay_het_han < CURRENT_DATE AND status = ? AND card_id = ?";
        List<Object[]> dataList = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(url, user, password); PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {

            preparedStatement.setString(1, "Chưa thanh toán");
            preparedStatement.setString(2, cardID);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String bookName = resultSet.getString("name");
                    String price = resultSet.getString("price");
                    Date ngayHetHan = resultSet.getDate("ngay_het_han");
                    double priceAmount = Double.parseDouble(price);
                    String formattedPrice = formatCurrency(priceAmount);

                    dataList.add(new Object[]{
                        id,
                        false,
                        bookName != null ? bookName : "",
                        formattedPrice,
                        ngayHetHan != null ? ngayHetHan.toString() : "",});
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
        }
        return dataList;
    }

    public static Object[][] fetchData(String cardID) {
        List<Object[]> dataList = fetchDataList(cardID);
        return dataList.toArray(Object[][]::new);
    }

    public static String formatCurrency(double amount) {
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        currencyFormat.setMinimumFractionDigits(0);
        currencyFormat.setMaximumFractionDigits(0);
        return currencyFormat.format(amount) + " VND";
    }

    @Override
    public void show() {
        String cardID = "";
        if (profilePanel != null && profilePanel.card_id != null) {
            cardID = profilePanel.card_id;
        }
        // Title Panel for Payment Table
        JPanel titlePanelPayment = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel tableTitlePayment = new JLabel("Thông tin sách quá hạn");
        tableTitlePayment.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 20));
        tableTitlePayment.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanelPayment.add(tableTitlePayment);

        // Table for Payment Information
        String[] columnNamesPayment = {"ID", "Chọn", "Tên sách", "Số tiền cần thanh toán", "Ngày hết hạn thanh toán"};
        Object[][] dataPayment = fetchData(cardID);

        modelPayment = new DefaultTableModel(dataPayment, columnNamesPayment) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Boolean.class;
                }
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };

        DefaultTableCellRenderer centerRendererPayment = new DefaultTableCellRenderer();
        centerRendererPayment.setHorizontalAlignment(SwingConstants.CENTER);

        tablePayment = new JTable(modelPayment);
        JTableHeader headerPayment = tablePayment.getTableHeader();
        headerPayment.setPreferredSize(new Dimension(0, 36));
        headerPayment.setFont(new Font("Arial", Font.BOLD, 14));
        headerPayment.setDefaultRenderer(new CustomHeaderRenderer());

        // Hide the ID column
        TableColumn idColumn = tablePayment.getColumnModel().getColumn(0);
        idColumn.setMinWidth(0);
        idColumn.setMaxWidth(0);
        idColumn.setPreferredWidth(0);

        // Center align the cells (except checkbox column)
        for (int i = 2; i < tablePayment.getColumnCount(); i++) {
            tablePayment.getColumnModel().getColumn(i).setCellRenderer(centerRendererPayment);
        }

        tablePayment.setFont(new Font("Arial", Font.PLAIN, 14));
        tablePayment.setRowHeight(36);

        // Scroll Pane for Table
        JScrollPane scrollPanePayment = new JScrollPane(tablePayment);
        scrollPanePayment.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Button Panel for Actions
        JPanel buttonPanelPayment = new JPanel();
        buttonPanelPayment.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // Create Buttons
        JButton buttonPayNow = new JButton("Thanh toán");
        JButton buttonRecharge = new JButton("Nạp tiền");

        // Set size and style for buttons
        buttonPayNow.setPreferredSize(new Dimension(150, 40));
        buttonPayNow.setBackground(new Color(0, 123, 255));
        buttonPayNow.setForeground(Color.WHITE);

        buttonRecharge.setPreferredSize(new Dimension(150, 40));
        buttonRecharge.setBackground(new Color(46, 139, 87));
        buttonRecharge.setForeground(Color.WHITE);

        buttonPayNow.addActionListener((ActionEvent e) -> {
            List<Integer> selectedIds = new ArrayList<>();
            double totalPayment = 0;
            for (int i = 0; i < modelPayment.getRowCount(); i++) {
                boolean isChecked = (Boolean) modelPayment.getValueAt(i, 1);
                if (isChecked) {
                    int id = (int) modelPayment.getValueAt(i, 0);
                    selectedIds.add(id);
                    String priceText = (String) modelPayment.getValueAt(i, 3);
                    double priceAmount = Double.parseDouble(priceText.replace(",", "").replace(" VND", ""));
                    totalPayment += priceAmount;
                }
            }
            if (selectedIds.isEmpty()) {
                JOptionPane.showMessageDialog(PaymentPanel.this, "Vui lòng chọn sách để thanh toán", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (totalPayment > balance) {
                HistoryPanel.insertRecord("Thanh toán", "Thất bại", profilePanel.card_id);
                

                JOptionPane.showMessageDialog(PaymentPanel.this, "Số dư không đủ để thanh toán", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (profilePanel != null && profilePanel.card_id != null) {
                String cardID1 = profilePanel.card_id;
                updateStatus(selectedIds, cardID1, totalPayment);  
                HistoryPanel.insertRecord("Thanh toán", "Thành công", profilePanel.card_id);

                JOptionPane.showMessageDialog(PaymentPanel.this, "Thanh toán thành công ", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                //Tải lại dữ liệu
                updateTable(cardID1);
            }
        });

        buttonRecharge.addActionListener((ActionEvent e) -> {
            showValidationDialog();
        });

        // Add buttons to the button panel
        buttonPanelPayment.add(buttonPayNow);
        buttonPanelPayment.add(buttonRecharge);
        // Update Content Panel
        contentPanel.removeAll();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(titlePanelPayment);
        contentPanel.add(scrollPanePayment);
        contentPanel.add(buttonPanelPayment);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void updateTable(String cardID) {
        Object[][] dataPayment = fetchData(cardID);
        modelPayment.setDataVector(dataPayment, new String[]{"ID", "Chọn", "Tên sách", "Số tiền cần thanh toán", "Ngày hết hạn thanh toán"});
    }

    private void updateStatus(List<Integer> selectedIds, String cardID, double totalPayment) {
        String updateSQL = "UPDATE sach_muon_tra SET status = ? WHERE id = ? AND card_id = ?";
        String updateBalanceSQL = "UPDATE card_info SET balance = balance - ? WHERE card_id = ?";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        PreparedStatement updateBalanceStatement = null;
        try {
            connection = DriverManager.getConnection(url, user, password);
            connection.setAutoCommit(false); // Start transaction

            //Update sách
            preparedStatement = connection.prepareStatement(updateSQL);
            for (int id : selectedIds) {
                preparedStatement.setString(1, "Đã thanh toán");
                preparedStatement.setInt(2, id);
                preparedStatement.setString(3, cardID);
                preparedStatement.executeUpdate();
            }
            //Update Balance
            updateBalanceStatement = connection.prepareStatement(updateBalanceSQL);
             updateBalanceStatement.setDouble(1, totalPayment);
                updateBalanceStatement.setString(2, cardID);
                updateBalanceStatement.executeUpdate();

            connection.commit(); // Commit transaction
        } catch (SQLException e) {
            if(connection != null){
                try{
                    connection.rollback();
                }catch (SQLException ex){
                  System.err.println("Lỗi rollback transaction: " + ex.getMessage());
                  JOptionPane.showMessageDialog(this, "Lỗi khi thực hiện thanh toán (rollback): " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
            System.err.println("Lỗi cập nhật trạng thái sách: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật trạng thái sách: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }finally {
           try{
               if(preparedStatement != null) preparedStatement.close();
               if (updateBalanceStatement != null) updateBalanceStatement.close();
               if (connection != null) connection.close();
           }catch (SQLException ex){
               System.err.println("Lỗi đóng kết nối: " + ex.getMessage());
               JOptionPane.showMessageDialog(this, "Lỗi khi đóng kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
           }
        }
    }

    private void showRechargeDialog() {
        JDialog rechargeDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Nạp tiền", true);
        rechargeDialog.setLayout(new GridBagLayout());
        rechargeDialog.setSize(350, 220);
        rechargeDialog.setResizable(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel amountLabel = new JLabel("Số tiền nạp:");
        amountLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        JTextField amountField = new JTextField(15);
        amountField.setPreferredSize(new Dimension(150, 32));
        amountField.setFont(new Font("Arial", Font.PLAIN, 14));
        amountField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        gbc.gridx = 0;
        gbc.gridy = 0;
        rechargeDialog.add(amountLabel, gbc);
        gbc.gridx = 1;
        rechargeDialog.add(amountField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton confirmButton = new JButton("Xác nhận");
        confirmButton.setPreferredSize(new Dimension(120, 40));
        confirmButton.setBackground(new Color(0, 123, 255));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setFont(new Font("Arial", Font.BOLD, 14));
        confirmButton.setFocusPainted(false);
        confirmButton.setBorder(BorderFactory.createEmptyBorder());

        JButton cancelButton = new JButton("Hủy bỏ");
        cancelButton.setPreferredSize(new Dimension(120, 40));
        cancelButton.setBackground(Color.GRAY);
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFont(new Font("Arial", Font.BOLD, 14));
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createEmptyBorder());

        confirmButton.addActionListener((ActionEvent e) -> {
            String amountText = amountField.getText();
            if (amountText.isEmpty()) {
                JOptionPane.showMessageDialog(rechargeDialog, "Vui lòng nhập số tiền", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                double amount = Double.parseDouble(amountText);
                if (amount <= 10000 || amount >= 10000000) {
                    JOptionPane.showMessageDialog(rechargeDialog, "Số tiền trong khoảng 10.000vnd và 10.000.000vnd", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    HistoryPanel.insertRecord("Nạp tiền", "Thất bại", profilePanel.card_id);
                    return;
                }
                if (profilePanel != null && profilePanel.card_id != null) {
                    String cardID = profilePanel.card_id;
                    updateBalance(cardID, amount);
                    String formattedAmount = formatCurrency(amount);
                    JOptionPane.showMessageDialog(rechargeDialog, "Nạp tiền thành công với số tiền " + formattedAmount, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    HistoryPanel.insertRecord("Nạp " + formattedAmount , "Thành công", card_id);

                    rechargeDialog.dispose();
                } else {
                    HistoryPanel.insertRecord("Nạp tiền", "Thất bại", profilePanel.card_id);

                    JOptionPane.showMessageDialog(rechargeDialog, "Không tìm thấy thông tin thẻ", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                HistoryPanel.insertRecord("Nạp tiền", "Thất bại", profilePanel.card_id);
                JOptionPane.showMessageDialog(rechargeDialog, "Số tiền nhập không đúng định dạng", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener((ActionEvent e) -> {
            rechargeDialog.dispose();
        });

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        rechargeDialog.add(buttonPanel, gbc);

        rechargeDialog.setLocationRelativeTo(this);
        rechargeDialog.setVisible(true);
    }

    private void updateBalance(String cardID, double amount) {
        String updateSQL = "UPDATE card_info SET balance = balance + ? WHERE card_id = ?";
        try (Connection connection = DriverManager.getConnection(url, user, password); PreparedStatement preparedStatement = connection.prepareStatement(updateSQL)) {
            preparedStatement.setDouble(1, amount);
            preparedStatement.setString(2, cardID);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật số dư: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật số dư", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
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
    private void showValidationDialog() {
        // Tạo dialog xác thực
        JDialog validationDialog = new JDialog((Frame) null, "Xác thực người dùng", true);
        validationDialog.setSize(500, 250);
        validationDialog.setLayout(new BorderLayout());

        // Panel phía trên chứa tiêu đề
        JPanel headerPanel = new JPanel();
        JLabel titleLabel = new JLabel("Xác thực người dùng");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(titleLabel);

        // Panel chính chứa checkbox
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(30, 30, 30, 30); // Tăng khoảng cách giữa các phần tử

// Tùy chỉnh kích thước của checkbox
        JCheckBox rsaCheckbox = new JCheckBox("Bạn đang muốn nạp tiền? Click tôi, hehe");
        rsaCheckbox.setFont(new Font("Arial", Font.BOLD, 20)); // Tăng font chữ
        rsaCheckbox.setPreferredSize(new Dimension(400, 100)); // Tăng kích thước tổng thể
        rsaCheckbox.setIcon(new ImageIcon(new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB))); // Tăng kích thước icon
        rsaCheckbox.setSelectedIcon(new ImageIcon(new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)));

// Thêm checkbox vào panel chính
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(rsaCheckbox, gbc);

        // Thêm xử lý logic cho checkbox
        rsaCheckbox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean isValid = validateCardWithRSA();
                if (isValid) {
                    rsaCheckbox.setText("Xác thực thành công");
                    rsaCheckbox.setForeground(new Color(0, 128, 0));
                    rsaCheckbox.setEnabled(false);
                    showRechargeDialog();
                    validationDialog.dispose();
                } else {
                    rsaCheckbox.setText("Xác thực thất bại");
                    rsaCheckbox.setForeground(Color.RED);
                }
            }
        });

        // Panel dưới chứa nút "Đóng"
        JPanel footerPanel = new JPanel();
        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(new Font("Arial", Font.PLAIN, 14));
        closeButton.addActionListener(e -> validationDialog.dispose());
        footerPanel.add(closeButton);

        // Thêm các panel vào dialog
        validationDialog.add(headerPanel, BorderLayout.NORTH);
        validationDialog.add(mainPanel, BorderLayout.CENTER);
        validationDialog.add(footerPanel, BorderLayout.SOUTH);

        validationDialog.setLocationRelativeTo(null);
        validationDialog.setVisible(true);
    }

    private BigInteger getModulusPublicKey() throws CardException {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminals terminals = factory.terminals();
        if (terminals.list().isEmpty()) {
            System.out.println("Không tìm thấy trình đọc thẻ.");
            return null;
        }

        CardTerminal terminal = terminals.list().get(0);
        Card card = terminal.connect("T=1");
        CardChannel channel = card.getBasicChannel();

        CommandAPDU getPubKeyCommand = new CommandAPDU(0xA4, 0x1A, 0x01, 0x01);
        ResponseAPDU response = channel.transmit(getPubKeyCommand);

        if (response.getSW() == 0x9000) {
            BigInteger res = new BigInteger(1, response.getData());
            System.out.println("responseM " + res);
            return res;
        } else {
            System.out.println("Không lấy được Public Key từ thẻ.");
            return null;
        }
    }

    private BigInteger getExponentPublicKey() throws CardException {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminals terminals = factory.terminals();
        if (terminals.list().isEmpty()) {
            System.out.println("Không tìm thấy trình đọc thẻ.");
            return null;
        }

        CardTerminal terminal = terminals.list().get(0);
        Card card = terminal.connect("T=1");
        CardChannel channel = card.getBasicChannel();

        CommandAPDU getPubKeyCommand = new CommandAPDU(0xA4, 0x1A, 0x02, 0x01);
        ResponseAPDU response = channel.transmit(getPubKeyCommand);

        if (response.getSW() == 0x9000) {
            BigInteger res = new BigInteger(1, response.getData());
            System.out.println("responseE " + res);
            return res;
        } else {
            System.out.println("Không lấy được Public Key từ thẻ.");
            return null;
        }
    }

    private boolean validateCardWithRSA() {
        try {
            // Khởi tạo kết nối với đầu đọc thẻ
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            if (terminals.list().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Không tìm thấy trình đọc thẻ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            CardTerminal terminal = terminals.list().get(0);
            Card card = terminal.connect("T=1");
            CardChannel channel = card.getBasicChannel();

            // Lấy mô-đun và số mũ công khai
            BigInteger modulusPubkey = getModulusPublicKey();
            BigInteger exponentPubkey = getExponentPublicKey();

            System.out.println("PublicKey Modulus (N): " + modulusPubkey);
            System.out.println("PublicKey Exponent (E): " + exponentPubkey);

            // Tạo PublicKey từ modulus và exponent
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulusPubkey, exponentPubkey);
            PublicKey public_Key = keyFactory.generatePublic(pubKeySpec);

            // Tạo chuỗi ngẫu nhiên để xác thực
            byte[] randomData = new byte[16]; // Chuỗi ngẫu nhiên 16 byte
            new java.security.SecureRandom().nextBytes(randomData); // Sinh chuỗi ngẫu nhiên an toàn
            System.out.println("Random Data (Input for Signing): " + randomData);

            // Gửi chuỗi ngẫu nhiên để thẻ ký
            CommandAPDU signCommand = new CommandAPDU(0xA4, 0x1B, 0x00, 0x00, randomData);
            ResponseAPDU signResponse = channel.transmit(signCommand);

            if (signResponse.getSW() != 0x9000) {
                JOptionPane.showMessageDialog(null, "Không thể lấy chữ ký từ thẻ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            byte[] signature = signResponse.getData();
            System.out.println("Signature from Card: " + signature);

            // Xác minh chữ ký
            Signature rsaVerify = Signature.getInstance("SHA1withRSA");
            rsaVerify.initVerify(public_Key);
            rsaVerify.update(randomData);

            if (rsaVerify.verify(signature)) {
                JOptionPane.showMessageDialog(null, "Xác thực thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "Xác thực thất bại.", "Thông báo", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Đã xảy ra lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static byte[] getCardID() throws CardException {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminals terminals = factory.terminals();
        if (terminals.list().isEmpty()) {
            System.out.println("Không tìm thấy trình đọc thẻ.");
            return null;
        }

        CardTerminal terminal = terminals.list().get(0);
        Card card = terminal.connect("T=1");
        CardChannel channel = card.getBasicChannel();

        CommandAPDU getCardIdCommand = new CommandAPDU(0xA4, 0x1D, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(getCardIdCommand);
        if (response.getSW() == 0x9000) {
            System.out.println("card_id:" + Arrays.toString(response.getData()));
            return response.getData();
        } else {
            System.out.println("Không lấy được Card ID từ thẻ.");
            return null;
        }
    }
}
