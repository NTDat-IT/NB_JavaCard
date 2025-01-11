package com.kma.librarycard;

import javax.swing.*;
import java.awt.*;

public class AppBar extends JPanel {

    private final JPanel rightAppBar;

    public AppBar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(getWidth(), 80));

        // Create left AppBar
        JPanel leftAppBar = createLeftAppBar();
        add(leftAppBar, BorderLayout.WEST);

        // Create right AppBar
        rightAppBar = createRightAppBar();
        add(rightAppBar, BorderLayout.EAST);
    }

    private JPanel createLeftAppBar() {
        JPanel leftAppBar = new JPanel();
        leftAppBar.setBackground(new Color(0, 28, 68));
        leftAppBar.setPreferredSize(new Dimension(200, 80));
        leftAppBar.setLayout(new GridBagLayout());

        ImageIcon libraryIcon = new ImageIcon("src/main/java/image/graduate_hat.png");
        Image scaledImage = libraryIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
        JLabel titleLabel = new JLabel("Thư viện");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);

        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(null);
        centerPanel.setOpaque(true);
        centerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        centerPanel.add(iconLabel);
        centerPanel.add(titleLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        leftAppBar.add(centerPanel, gbc);
        return leftAppBar;
    }
     private static JPanel createRightAppBar() {
        JPanel rightAppBar = new JPanel();
        rightAppBar.setLayout(new BorderLayout());
        return rightAppBar;
    }

    public JPanel getRightAppBar() {
        return rightAppBar;
    }
}