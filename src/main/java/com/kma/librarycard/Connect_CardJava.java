package com.kma.librarycard;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.smartcardio.*;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Connect_CardJava extends JFrame {

    public static final byte[] AID_APPLET = {(byte) 0x24, (byte) 0x02, (byte) 0x05, (byte) 0x01, (byte) 0x09, (byte) 0x00};
    private Card card;
    private TerminalFactory factory;
    private CardChannel channel;
    private CardTerminal terminal;
    private List<CardTerminal> terminals;
    private ResponseAPDU response;
    private String cardId = null;
    private static boolean connectFrameShown = false;
    private static boolean otpInputShown = false;

    public Connect_CardJava() {
        setVisible(true);
        
        if (!connectFrameShown) {
            showConnectCardFrame();
        }


         // Thêm WindowListener để gọi disconnect khi đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectCard(); // Call disconnect when window is closing
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });

      // Thêm Shutdown Hook (Backup in case Window Closing does not get executed)
        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnectCard));


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

    private synchronized void showConnectCardFrame() {
        if (connectFrameShown) {
            return; // Chỉ hiển thị nếu chưa được mở
        }
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
                        byte[] card_ID = null;
                        try {
                            card_ID = getCardID();
                            if(card_ID != null) {
                                 cardId = new String(card_ID, StandardCharsets.UTF_8);
                            }
                            
                            HistoryPanel.insertRecord("Kết nối thẻ", "Thành công", cardId);
                        } catch (CardException ex) {
                            Logger.getLogger(Connect_CardJava.class.getName()).log(Level.SEVERE, null, ex);
                        }

                         if (!otpInputShown) {
                            otpInputShown = true; // Đánh dấu đã mở OTPInput
                            OTPInput otpInput = new OTPInput();
                             otpInput.setVisible(true);
                       }

                    } else {
                        messageLabel.setText("Kết nối thẻ thất bại!");
                        JOptionPane.showMessageDialog(connectFrame, "Không thể kết nối với thẻ. Đóng ứng dụng.");
                       if(cardId != null){
                         HistoryPanel.insertRecord("Kết nối thẻ", "Thất bại", cardId);
                      }

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
            if (terminals.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy đầu đọc thẻ. Hãy đảm bảo đầu đọc đã được kết nối.");
                return false;
            }
            terminal = terminals.get(0);
            card = terminal.connect("T=1");
            channel = card.getBasicChannel();
            if (channel == null) {
                return false;
            }
            response = channel.transmit(new CommandAPDU(0x00, (byte) 0xA4, 0x04, 0x00, AID_APPLET));
            if (response.getSW() == 0x9000) {
                return true;
            } else {
                JOptionPane.showMessageDialog(this, "Không kết nối được với applet trên thẻ.");
                return false;
            }
        } catch (CardException ex) {
            Logger.getLogger(Connect_CardJava.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    // Phương thức ngắt kết nối thẻ
   private void disconnectCard() {
    if (card != null && channel != null) {
        try {
            card.disconnect(true);
            System.out.println("Thẻ đã ngắt kết nối");
            if(cardId != null){
              HistoryPanel.insertRecord("Ngắt kết nối thẻ", "Thành công", cardId);
            }

        } catch (CardException e) {
                System.err.println("Lỗi khi ngắt kết nối thẻ: " + e.getMessage());
                if(cardId != null){
                     HistoryPanel.insertRecord("Ngắt kết nối thẻ", "Thất bại", cardId);
                   }
        } finally {
            card = null;
            channel = null;
        }
     }
  }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Connect_CardJava::new);
    }
}