/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kma.librarycard;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.smartcardio.*;
import javax.swing.JOptionPane;

/**
 *
 * @author lamtr
 */
public class OTPInput extends JFrame {

    public static final byte[] AID_APPLET = {(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x01};
    private Card card;
    private TerminalFactory factory;
    private CardChannel channel;
    private CardTerminal terminal;
    private List<CardTerminal> terminals;
    private ResponseAPDU response;

    private int failedAttempts = 0;

    public OTPInput() {
        // Thiết lập cửa sổ
        setTitle("Nhập mã pin");
        setSize(600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE); // Màu nền trắng cho cửa sổ chính

        // Tạo panel chính
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout()); // Đổi layout thành GridBagLayout
        mainPanel.setBackground(Color.WHITE); // Màu nền trắng cho panel chính

        // Panel cho các ô nhập OTP
        JPanel otpPanel = new JPanel();
        otpPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Căn giữa các ô nhập với khoảng cách
        otpPanel.setBackground(Color.WHITE); // Màu nền trắng cho panel nhập OTP

        // Tạo 4 ô nhập OTP với kiểu dáng bo góc
        JPasswordField otp1 = createRoundedPasswordField();
        JPasswordField otp2 = createRoundedPasswordField();
        JPasswordField otp3 = createRoundedPasswordField();
        JPasswordField otp4 = createRoundedPasswordField();

        // Thêm logic tự động chuyển ô
        addAutoMove(otp1, null, otp2); // otp1 không có ô trước
        addAutoMove(otp2, otp1, otp3);
        addAutoMove(otp3, otp2, otp4);
        addAutoMove(otp4, otp3, null); // otp4 không có ô sau

        // Thêm các ô nhập vào panel
        otpPanel.add(otp1);
        otpPanel.add(otp2);
        otpPanel.add(otp3);
        otpPanel.add(otp4);

        // Nút "Tiếp tục"
        JButton continueButton = new JButton("Tiếp tục");

        // Thay đổi màu nền của nút thành màu đỏ
        continueButton.setBackground(new Color(0, 28, 68)); // Màu nền mới cho nút

        // Đặt kích thước của nút là 300x100
        continueButton.setPreferredSize(new Dimension(240, 75));

        // Thiết lập màu chữ và các thuộc tính khác
        continueButton.setFont(new Font("Arial", Font.BOLD, 18)); // Chữ đậm, kích thước 18
        continueButton.setForeground(Color.WHITE); // Màu chữ trắng

        // Bo viền của nút
        continueButton.setBorder(new RoundedBorder(20)); // Bán kính bo góc là 20

        // Thêm hành động cho nút
//        continueButton.addActionListener(e -> {
//            // Lấy giá trị từ các ô nhập OTP
//            String otp = new String(otp1.getPassword())
//                    + new String(otp2.getPassword())
//                    + new String(otp3.getPassword())
//                    + new String(otp4.getPassword());
//            JOptionPane.showMessageDialog(OTP_Test.this, "Mã OTP: " + otp);
//        });
        //push sang màn hình
        continueButton.addActionListener(e -> {

            String otp = new String(otp1.getPassword()) + new String(otp2.getPassword()) + new String(otp3.getPassword()) + new String(otp4.getPassword());

            // Kiểm tra OTP với thẻ thông minh
            boolean isOtpValid = checkOTPWithCard(otp);

            if (isOtpValid) {
                // Nếu OTP đúng, chuyển đến trang chính
                MainPage mainPage = new MainPage();
                mainPage.setVisible(true);
                mainPage.setValidatedOtp(otp);
                OTPInput.this.dispose();  // Đóng cửa sổ hiện tại
            } else {
                // Nếu OTP sai quá 3 lần, vô hiệu hóa thẻ
                failedAttempts++;
                if (failedAttempts >= 3) {
                    JOptionPane.showMessageDialog(OTPInput.this, "Sai OTP quá 3 lần, thẻ đã bị vô hiệu hóa!");
                    System.exit(0);
                } else {
                    JOptionPane.showMessageDialog(OTPInput.this, "OTP sai. Thử lại.");
                }
            }
        });

        // Thêm nút vào giao diện
        mainPanel.add(continueButton);

        // Thêm panel vào frame
        add(mainPanel);

        // Tạo hình ảnh với kích thước 150x150
        ImageIcon originalIcon = new ImageIcon("src//main//java//image//password.png");
        Image scaledImage = originalIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

// Thêm hình ảnh vào JLabel
        JLabel imageLabel = new JLabel(scaledIcon);
        imageLabel.setHorizontalAlignment(JLabel.CENTER); // Căn giữa hình ảnh

        // Thêm dòng text "Nhập mã PIN"
        JLabel textLabel = new JLabel("Nhập mã PIN");
        textLabel.setFont(new Font("Arial", Font.BOLD, 24)); // Font chữ lớn
        textLabel.setHorizontalAlignment(JLabel.CENTER); // Căn giữa

        // Thêm panel chứa các ô nhập OTP vào giữa
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 20, 20, 20); // Cách lề giữa các thành phần
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(imageLabel, gbc); // Thêm hình ảnh vào panel

        // Thêm dòng text "Nhập mã PIN"
        gbc.gridy = 1;
        mainPanel.add(textLabel, gbc); // Thêm dòng text vào panel

        // Thêm panel OTP vào phía dưới dòng text
        gbc.gridy = 2;
        mainPanel.add(otpPanel, gbc);

        // Thêm nút "Tiếp tục" vào bên dưới
        gbc.gridy = 3;
        mainPanel.add(continueButton, gbc);

        // Thêm panel chính vào frame
        add(mainPanel);
    }

    private JPasswordField createRoundedPasswordField() {
        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(80, 80));
        passwordField.setHorizontalAlignment(JTextField.CENTER);
        passwordField.setDocument(new JTextFieldLimit(1));

        // Thiết lập kích thước chữ
        passwordField.setFont(new Font("Arial", Font.BOLD, 40)); // Chữ đậm, kích thước 18

        // Hiển thị dấu * thay cho ký tự nhập
        passwordField.setEchoChar('*');

        // Màu nền bên trong là xám
        passwordField.setBackground(Color.LIGHT_GRAY); // Màu nền xám cho ô nhập OTP

        // Áp dụng viền bo góc
        passwordField.setBorder(new RoundedBorder(10)); // Bán kính bo góc là 10

        return passwordField;
    }

    private boolean checkOTPWithCard(String otp) {
        try {
            // Kết nối tới trình đọc thẻ
            factory = TerminalFactory.getDefault();
            terminals = factory.terminals().list();
            terminal = terminals.get(0);
            card = terminal.connect("T=1");
            channel = card.getBasicChannel();
            if (terminals.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy trình đọc thẻ.");
                return false;
            }
            // Gửi lệnh VERIFY OTP
            byte[] otpBytes = otp.getBytes();
            System.out.println(otpBytes);
            CommandAPDU verifyOTP = new CommandAPDU(0xA4, 0x05, 0x00, 0x00, otpBytes);

            response = channel.transmit(verifyOTP);
            System.out.println("Sau khi da gui lenh");

            return response.getSW() == 0x9000;
        } catch (HeadlessException | CardException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi giao tiếp với thẻ.");
            return false;
        }
    }

    // Hàm thêm logic tự động chuyển sang ô tiếp theo hoặc quay lại ô trước
    private void addAutoMove(JPasswordField currentField, JPasswordField previousField, JPasswordField nextField) {
        currentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (currentField.getPassword().length == 0 && e.getKeyChar() != KeyEvent.VK_BACK_SPACE) {
                    if (nextField != null) {
                        SwingUtilities.invokeLater(nextField::requestFocus);
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && currentField.getPassword().length == 0) {
                    if (previousField != null) {
                        SwingUtilities.invokeLater(() -> {
                            previousField.requestFocus();
                            previousField.setText(""); // Xóa ký tự nếu có
                        });
                    }
                }
            }
        });
    }

}

// Lớp tùy chỉnh để tạo viền bo góc
class RoundedBorder extends AbstractBorder {

    private final int radius;

    public RoundedBorder(int radius) {
        this.radius = radius;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(Color.GRAY); // Màu viền
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius); // Vẽ viền bo góc
        g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(radius + 1, radius + 1, radius + 1, radius + 1);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.right = insets.top = insets.bottom = radius + 1;
        return insets;
    }
}

// Giới hạn số ký tự trong ô JTextField
class JTextFieldLimit extends javax.swing.text.PlainDocument {

    private final int limit;

    JTextFieldLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public void insertString(int offset, String str, javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
        if (str == null) {
            return;
        }

        if ((getLength() + str.length()) <= limit) {
            super.insertString(offset, str, attr);
        }
    }
}
