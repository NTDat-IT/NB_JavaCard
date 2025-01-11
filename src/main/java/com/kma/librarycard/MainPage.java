package com.kma.librarycard;

import com.kma.librarycard.components.*;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

public final class MainPage extends JFrame {

    private final JPanel contentPanel;
    private final JPanel rightAppBar;
    private String validatedOtp = "";

    static String url = "jdbc:mysql://localhost:3306/lib_javacard?zeroDateTimeBehavior=CONVERT_TO_NULL";
    static String user = "root";
    static String password = "";

    public void setValidatedOtp(String validatedOtp) {
        this.validatedOtp = validatedOtp;
    }
    public MainPage() {
        conn(); // Database connection
        setTitle("Trang chủ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Create AppBar
        AppBar appBar = new AppBar();
        rightAppBar = appBar.getRightAppBar();
        add(appBar, BorderLayout.NORTH);

        // Create TabBar
        TabBar tabBar = new TabBar(this);
        add(tabBar, BorderLayout.WEST);

        // Create content panel
        contentPanel = new JPanel();
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setLayout(new BorderLayout());
        JLabel initialContent = new JLabel("Chào mừng bạn đến với hệ thống quản lý thư viện! :).");
        initialContent.setHorizontalAlignment(SwingConstants.CENTER);
        initialContent.setFont(new Font("Arial", Font.BOLD, 20));
        contentPanel.add(initialContent, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        setVisible(true);
    }
// thêm trườn balance lấy tron dataBase

    private double getBalanceForDataBase(String cardID) {
        double balance = 0.0;
        String selectSQL = "SELECT balance FROM card_info WHERE card_id = ?";

        try (Connection connection = DriverManager.getConnection(url, user, password); PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {

            preparedStatement.setString(1, cardID);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    balance = resultSet.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi truy vấn số dư: " + e.getMessage());
            // You might choose to throw a custom exception here to handle it on the call level
        }
        return balance;
    }

    public void switchTab(String selectedMenu) {
        String infoCard = getInfoCard();
        String nameCard = "";
        String idCard = "";
        String addressCard = "";
        String phoneCard = "";
        String pinCard = "";
        Double balance = null;
        try {
            String[] parts = infoCard.split("\u0003");
            System.out.println(Arrays.toString(parts));
            if (parts.length == 4) {
                idCard = parts[0];
                nameCard = parts[1];
                addressCard = parts[2];
                phoneCard = parts[3];
                balance = getBalanceForDataBase(idCard);
            } else {
                throw new IllegalArgumentException("Dữ liệu thẻ không hợp lệ: số lượng phần tử không đúng.");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Lỗi: Dữ liệu thẻ không đầy đủ.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println("Đã xảy ra lỗi không mong muốn.");
        }
        System.out.println("idCard: " + idCard);
        System.out.println("nameCard: " + nameCard);
        System.out.println("addressCard: " + addressCard);
        System.out.println("phoneCard: " + phoneCard);
        System.out.println("pinCard: " + pinCard);

        if (rightAppBar != null) {
            rightAppBar.removeAll();
            contentPanel.removeAll();
            switch (selectedMenu) {
                case "Thông tin cá nhân" -> {
                    ProfilePanel profilePanel = new ProfilePanel(contentPanel, idCard, nameCard, addressCard, phoneCard, validatedOtp, balance, this);
                    profilePanel.show();
                }
                case "Trang chủ" -> {
                    DashboardPanel dashboardPanel = new DashboardPanel(contentPanel,balance,idCard);
                    dashboardPanel.show();
                }
                case "Mượn trả sách" -> {
                    // Create a dummy ProfilePanel instance.
                    ProfilePanel profilePanel = new ProfilePanel(contentPanel, idCard, nameCard, addressCard, phoneCard, validatedOtp, balance, this); // Pass this if MainPage is not null
                    BorrowReturnPanel borrowReturnPanel = new BorrowReturnPanel(contentPanel, profilePanel);
                    borrowReturnPanel.show();
                }
                case "Sách quá hạn" -> {
                    ProfilePanel profilePanel = new ProfilePanel(contentPanel, idCard, nameCard, addressCard, phoneCard, validatedOtp, balance, this); // Pass this if MainPage is not null
                    PaymentPanel paymentPanel = new PaymentPanel(contentPanel, profilePanel, balance); // Thay vì chỉ có contentPanel
                    paymentPanel.show();
                }
                case "Lịch sử hoạt động" -> {
                    HistoryPanel historyPanel = new HistoryPanel(contentPanel,idCard);
                    historyPanel.show();
                }
                default -> {
                    JLabel newContent = new JLabel("Welcome");
                    newContent.setHorizontalAlignment(SwingConstants.CENTER);
                    newContent.setFont(new Font("Arial", Font.BOLD, 20));
                    contentPanel.add(newContent, BorderLayout.CENTER);
                }
            }

            rightAppBar.revalidate();
            rightAppBar.repaint();
            contentPanel.revalidate();
            contentPanel.repaint();
        }
    }

    public Connection conn() {
        Connection connection = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Kết nối database thành công!");
        } catch (ClassNotFoundException e) {
            System.out.println(e);
            System.err.println("Lỗi: Không tìm thấy driver JDBC. " + e.getMessage());

        } catch (SQLException e) {
            System.err.println("Lỗi: Không thể kết nối tới database. " + e.getMessage());
        }
        return connection;
    }

    private String getInfoCard() {
        String chuoiByte = "";
        try {
            // Kết nối đến thẻ
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            if (terminals.list().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Không tìm thấy trình đọc thẻ.");

            }
            CardTerminal terminal = terminals.list().get(0);
            Card card = terminal.connect("T=1");
            CardChannel channel = card.getBasicChannel();

            // Gửi lệnh APDU để chọn ứng dụng của thẻ
            CommandAPDU getNameCommand = new CommandAPDU(0xA4, 0x16, 0x00, 0x00);

            ResponseAPDU nameResponse = channel.transmit(getNameCommand);

            if (nameResponse.getSW() == 0x9000) {
                byte[] data = nameResponse.getData();
                System.out.println("Dữ liệu nhận được từ thẻ: " + Arrays.toString(data));
                chuoiByte = new String(data, "UTF-8");
            } else {
//                JOptionPane.showMessageDialog(this, "Không thể đọc tên từ thẻ.");
                return chuoiByte;
            }
        } catch (CardException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi kết nối với thẻ.");
        } catch (UnsupportedEncodingException e) {
            JOptionPane.showMessageDialog(this, "Lỗi giải mã dữ liệu từ thẻ.");
        }
        return chuoiByte;
    }

    public void changeImage(byte[] image) {
        int length = image.length;
        int pointer = 0;
        boolean first = true;

        while (pointer < length) {
            int size = Math.min(length - pointer, 128); // Điều chỉnh kích thước nếu cần
            byte[] buf = new byte[size];
            System.arraycopy(image, pointer, buf, 0, size);

            try {
                // Kết nối với thẻ
                TerminalFactory factory = TerminalFactory.getDefault();
                CardTerminal terminal = factory.terminals().list().get(0);
                Card card = terminal.connect("T=1");
                CardChannel channel = card.getBasicChannel();

                // Xác định P2
                int p2 = pointer + size < length ? (first ? 0x00 : 0x01) : 0x02;

                // Tạo lệnh APDU
                CommandAPDU command = new CommandAPDU(0xA4, 0x18, 0x00, p2, buf);
                ResponseAPDU response = channel.transmit(command);

                // Kiểm tra phản hồi
                if (response.getSW() == 0x9000) {
                    System.out.println("Gửi dữ liệu thành công!");
                } else {
                    System.err.println("Lỗi khi gửi dữ liệu: " + Integer.toHexString(response.getSW()));
                    break; // Dừng khi gặp lỗi
                }
            } catch (Exception e) {
                e.printStackTrace();
                break; // Dừng khi gặp lỗi
            }

            first = false;
            pointer += size;
        }
    }

    public byte[] getImage() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int pointer = 0;
        boolean first = true;
        byte[] data = new byte[0];

        try {
            // Kết nối với thẻ
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminal terminal = factory.terminals().list().get(0);
            Card card = terminal.connect("T=1");
            CardChannel channel = card.getBasicChannel();

            // Lấy dữ liệu từ thẻ cho đến khi không còn dữ liệu
            while (true) {
                // Xác định P2 và kích thước
                int p2 = first ? 0x00 : 0x01; // P2 = 0x00 cho gói đầu tiên, P2 = 0x01 cho các gói tiếp theo
                CommandAPDU command = new CommandAPDU(0xA4, 0x19, 0x00, p2, new byte[0]);
                ResponseAPDU response = channel.transmit(command);

                // Kiểm tra phản hồi
                if (response.getSW() == 0x9000) {
                    byte[] responseData = response.getData();
                    if (responseData.length == 0) {
                        break; // Dừng khi không còn dữ liệu
                    }

                    // Ghi dữ liệu nhận được vào outputStream
                    outputStream.write(responseData);

                    // Cập nhật biến first
                    first = false;
                } else {
                    System.err.println("Lỗi khi nhận dữ liệu: " + Integer.toHexString(response.getSW()));
                    break; // Dừng khi gặp lỗi
                }
            }

            // Chuyển dữ liệu đã nhận vào mảng byte
            data = outputStream.toByteArray();

        } catch (Exception e) {
        }

        return data;
    }

    public void changePin(String oldPin, String newPin) {
        byte[] oldPinBytes = oldPin.getBytes();
        byte[] newPinBytes = newPin.getBytes();
        int lc = oldPinBytes.length + 1 + newPinBytes.length; // oldPin + separator + newPin

        try {
            // Kết nối đến thẻ
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();

            if (terminals.list().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Không tìm thấy trình đọc thẻ.");
                return;
            }

            CardTerminal terminal = terminals.list().get(0);
            Card card = terminal.connect("T=1");
            CardChannel channel = card.getBasicChannel();

            // Tạo lệnh APDU để thay đổi mã PIN
            byte[] command = new byte[5 + lc];
            command[0] = (byte) 0xA4;  // CLA
            command[1] = (byte) 0x14;  // INS
            command[2] = 0x00;         // P1
            command[3] = 0x00;         // P2
            command[4] = (byte) lc;    // LC
            System.arraycopy(oldPinBytes, 0, command, 5, oldPinBytes.length);
            command[5 + oldPinBytes.length] = (byte) 0x03; // Separator
            System.arraycopy(newPinBytes, 0, command, 6 + oldPinBytes.length, newPinBytes.length);

            // Gửi lệnh APDU thay đổi mã PIN
            CommandAPDU changePinCommand = new CommandAPDU(command);
            ResponseAPDU response = channel.transmit(changePinCommand);

            // Kiểm tra phản hồi từ thẻ
            if (response.getSW() == 0x9000) {
                JOptionPane.showMessageDialog(null, "Thay đổi mã PIN thành công!");
            } else {
                JOptionPane.showMessageDialog(null, "Thay đổi mã PIN thất bại. Mã lỗi: "
                        + Integer.toHexString(response.getSW()));
            }

        } catch (CardException e) {
            JOptionPane.showMessageDialog(null, "Lỗi khi kết nối với thẻ.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Lỗi không xác định.");
        }
    }

    private byte[] convertImageToBytes(File imageFile) throws IOException {
        ByteArrayOutputStream baos;
        try ( // Đọc file hình ảnh vào mảng byte
                FileInputStream fis = new FileInputStream(imageFile)) {
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            // Đảm bảo đóng stream sau khi sử dụng
        }
        baos.close();

        return baos.toByteArray();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainPage::new);
    }
}
