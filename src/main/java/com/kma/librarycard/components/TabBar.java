package com.kma.librarycard.components;


import com.kma.librarycard.MainPage;
import javax.swing.*;
import java.awt.*;

import javax.swing.border.Border;

public class TabBar extends JPanel {


    public TabBar(MainPage mainPage) {
        setBackground(new Color(0, 28, 68));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Create buttons with icons
        JButton tab1 = createTabButtonWithIcon("Trang chủ", "src/main/java/image/home.png");
        JButton tab2 = createTabButtonWithIcon("Thông tin cá nhân", "src/main/java/image/user.png");
        JButton tab3 = createTabButtonWithIcon("Mượn trả sách", "src/main/java/image/book.png");
        JButton tab4 = createTabButtonWithIcon("Sách quá hạn", "src/main/java/image/payment.png");
        JButton tab5 = createTabButtonWithIcon("Lịch sử hoạt động", "src/main/java/image/history.png");

        // Attach actions to buttons
        tab1.addActionListener(e -> mainPage.switchTab("Trang chủ"));
        tab2.addActionListener(e -> mainPage.switchTab("Thông tin cá nhân"));
        tab3.addActionListener(e -> mainPage.switchTab("Mượn trả sách"));
        tab4.addActionListener(e -> mainPage.switchTab("Sách quá hạn"));
        tab5.addActionListener(e -> mainPage.switchTab("Lịch sử hoạt động"));

        add(tab1);
        add(tab2);
        add(tab3);
        add(tab4);
        add(tab5);

    }


    private JButton createTabButtonWithIcon(String text, String iconPath) {
        ImageIcon originalIcon = new ImageIcon(iconPath);
        Image scaledImage = originalIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        JButton button = new JButton(text, scaledIcon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setPreferredSize(new Dimension(200, 50));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBackground(new Color(0, 28, 68));
        button.setOpaque(true);
        Border blackBorder = BorderFactory.createLineBorder(Color.BLACK, 3);
        button.setBorder(blackBorder);
        button.setForeground(Color.WHITE);
        button.setIconTextGap(15);
        return button;
    }
    
}