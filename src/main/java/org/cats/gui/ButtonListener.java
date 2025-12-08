package org.cats.gui;

import org.cats.scan.StartScanner;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ButtonListener implements ActionListener {
    Initialize gui;

    protected ButtonListener(Initialize gui) {
        this.gui = gui;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String ip = gui.getIp();
        String portsText = gui.getPorts();

        if (ip.isEmpty() || portsText.isEmpty()) {
            JOptionPane.showMessageDialog(gui, "Введите IP и диапазон портов!");
            return;
        }

        String[] range = portsText.split("-");
        if (range.length != 2) {
            JOptionPane.showMessageDialog(gui, "Диапазон портов должен быть в формате start-end, например 25565-25570");
            return;
        }

        int startPort;
        int endPort;
        try {
            startPort = Integer.parseInt(range[0].trim());
            endPort = Integer.parseInt(range[1].trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(gui, "Порты должны быть числами!");
            return;
        }

        String threadsStr = JOptionPane.showInputDialog(gui, "Введите количество потоков:");
        if (threadsStr == null) return;

        int threads;
        try {
            threads = Integer.parseInt(threadsStr.trim());
            if (threads <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(gui, "Некорректное количество потоков!");
            return;
        }

        new Thread(() -> {
            StartScanner scanner = new StartScanner(threads);
            scanner.scan(ip, startPort, endPort, Initialize.getConsole());
        }).start();

    }
}
