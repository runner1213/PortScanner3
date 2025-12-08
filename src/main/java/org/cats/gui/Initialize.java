package org.cats.gui;

import javax.swing.*;
import java.awt.*;

public class Initialize extends JFrame {

    private JTextField ipField;
    private JTextField portsField;

    private static ConsolePanel console;
    private JButton startButton;

    public Initialize() {
        super("PortScanner");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 800);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel title = new JLabel("Сканер портов", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.PLAIN, 32));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(title);
        topPanel.add(Box.createVerticalStrut(25));

        JPanel inputs = new JPanel(new GridLayout(1, 2, 15, 0));
        ipField = new JTextField();
        portsField = new JTextField();

        Dimension textSize = new Dimension(250, 32); // 32 по умолчанию
        ipField.setPreferredSize(textSize);
        portsField.setPreferredSize(textSize);

        inputs.add(labeled(ipField, "IP / Домен"));
        inputs.add(labeled(portsField, "Диапазон (пример: 1-65535)"));

        topPanel.add(inputs);
        topPanel.add(Box.createVerticalStrut(20));

        startButton = new JButton("Начать");
        startButton.setFont(new Font("Arial", Font.PLAIN, 14));
        startButton.setPreferredSize(new Dimension(800, 50));
        startButton.setMaximumSize(new Dimension(800, 50));
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(startButton);
        topPanel.add(Box.createVerticalStrut(20));

        add(topPanel, BorderLayout.NORTH);

        console = new ConsolePanel();
        add(console, BorderLayout.CENTER);

        startButton.addActionListener(new ButtonListener(this));

        setLocationRelativeTo(null);
        setVisible(true);

        ipField.setEnabled(true);
        console.print("Успешная инициализация. Готово к использованию");
    }

    private JPanel labeled(JComponent field, String labelText) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(field);

        return panel;
    }

    public String getIp() {
        return ipField.getText();
    }

    public String getPorts() {
        return portsField.getText();
    }

    public static ConsolePanel getConsole() {
        return console;
    }
}
