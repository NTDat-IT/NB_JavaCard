package com.kma.librarycard;

import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.smartcardio.*;
import javax.swing.*;

public class Connect_CardJava extends JFrame {

    public static final byte[] AID_APPLET = {(byte) 0x24, (byte) 0x02, (byte) 0x05, (byte) 0x01, (byte) 0x09, (byte) 0x00};

    private Card card;
    private TerminalFactory factory;
    private CardChannel channel;
    private CardTerminal terminal;
    private List<CardTerminal> terminals;
    private ResponseAPDU response;

    private static boolean connectFrameShown = false; // Kiểm soát ConnectFrame
    private static boolean otpInputShown = false; // Kiểm soát OTPInput

    private JPanel contentPanel;

    public Connect_CardJava() {

        setVisible(true);

        if (!connectFrameShown) {
            showConnectCardFrame();
        }
    }

    // Tạo TabBar
    private JPanel createTabBar() {
        JPanel tabBar = new JPanel();
        tabBar.setBackground(new Color(200, 200, 200)); // Màu nền của TabBar
        tabBar.setLayout(new BoxLayout(tabBar, BoxLayout.Y_AXIS)); // Xếp dọc các nút

        // Tạo các nút
        JButton tab1 = createTabButton("Tab 1");
        JButton tab2 = createTabButton("Tab 2");
        JButton tab3 = createTabButton("Tab 3");
        JButton tab4 = createTabButton("Tab 4");
        JButton tab5 = createTabButton("Tab 5");

        // Gắn sự kiện cho từng nút
        tab1.addActionListener(e -> switchContent("Đây là nội dung Tab 1"));
        tab2.addActionListener(e -> switchContent("Đây là nội dung Tab 2"));
        tab3.addActionListener(e -> switchContent("Đây là nội dung Tab 3"));
        tab4.addActionListener(e -> switchContent("Đây là nội dung Tab 4"));
        tab5.addActionListener(e -> switchContent("Đây là nội dung Tab 5"));

        // Thêm các nút vào TabBar
        tabBar.add(tab1);
        tabBar.add(tab2);
        tabBar.add(tab3);
        tabBar.add(tab4);
        tabBar.add(tab5);

        return tabBar;
    }

    // Phương thức tạo nút với chiều cao cố định
    private JButton createTabButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(200, 50)); // Chiều cao 50, chiều rộng 200
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); // Chiều cao 50
        button.setAlignmentX(Component.CENTER_ALIGNMENT); // Canh giữa theo trục ngang
        button.setFont(new Font("Arial", Font.BOLD, 16)); // Font chữ lớn hơn
        return button;
    }

    // Chuyển nội dung hiển thị khi chọn Tab
    private void switchContent(String content) {
        contentPanel.removeAll();
        JLabel newContent = new JLabel(content);
        newContent.setHorizontalAlignment(SwingConstants.CENTER);
        newContent.setFont(new Font("Arial", Font.BOLD, 20));
        contentPanel.add(newContent, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private synchronized void showConnectCardFrame() {
        if (connectFrameShown) return; // Chỉ hiển thị nếu chưa được mở
        connectFrameShown = true;

        JFrame connectFrame = new JFrame("Kết nối thẻ");
        connectFrame.setSize(400, 200);
        connectFrame.setLocationRelativeTo(this);
        connectFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        connectFrame.setLayout(new BorderLayout());

        JLabel messageLabel = new JLabel("Đang kết nối với thẻ, vui lòng chờ...");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        connectFrame.add(messageLabel, BorderLayout.CENTER);

        // Giả lập kết nối thẻ (thời gian trễ 2 giây)
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Giả lập thời gian kết nối
                SwingUtilities.invokeLater(() -> {
                    boolean connected = connectToCard();
                    if (connected) {
                        messageLabel.setText("Kết nối thẻ thành công!");
                        JOptionPane.showMessageDialog(connectFrame, "Thẻ đã kết nối thành công!");
                        connectFrame.dispose();

                        if (!otpInputShown) {
                            otpInputShown = true; // Đánh dấu đã mở OTPInput
                            OTPInput otpInput = new OTPInput();
                            otpInput.setVisible(true);
                        }
                    } else {
                        messageLabel.setText("Kết nối thẻ thất bại!");
                        JOptionPane.showMessageDialog(connectFrame, "Không thể kết nối với thẻ. Đóng ứng dụng.");
                        System.exit(0);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        connectFrame.setVisible(true);
    }

    private boolean connectToCard() {
        try {
            factory = TerminalFactory.getDefault();
            terminals = factory.terminals().list();
            terminal = terminals.get(0);
            card = terminal.connect("T=0");
            channel = card.getBasicChannel();
            if (channel == null) {
                return false;
            }
            response = channel.transmit(new CommandAPDU(0x00, (byte) 0xA4, 0x04, 0x00, AID_APPLET));
            return response.getSW() == 0x9000;
        } catch (CardException ex) {
            Logger.getLogger(Connect_CardJava.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Connect_CardJava::new);
    }
}
