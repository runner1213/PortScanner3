package org.cats.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ConsolePanel extends JPanel {

    private final JTextArea console;

    public ConsolePanel() {
        setLayout(new BorderLayout());

        console = new JTextArea();
        console.setEditable(false);
        console.setFont(new Font("Consolas", Font.PLAIN, 13));
        //console.setBackground(Color.BLACK);

        JScrollPane scroll = new JScrollPane(console);
        scroll.setBorder(new TitledBorder("Консольный вывод"));
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scroll, BorderLayout.CENTER);
    }

    /** Вывод в консоль **/
    public void print(String text) {
        console.append(text + "\n");
        console.setCaretPosition(console.getDocument().getLength()); // автоскролл
    }
}
