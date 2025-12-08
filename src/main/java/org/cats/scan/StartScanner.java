package org.cats.scan;

import org.cats.gui.ConsolePanel;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StartScanner {

    private final int threads;

    public StartScanner(int threads) {
        this.threads = threads;
    }

    public void scan(String ip, int startPort, int endPort, ConsolePanel console) {
        console.print("Старт сканирования " + ip + " (" + startPort + "-" + endPort + ") потоков: " + threads);

        List<Integer> openPorts = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int port = startPort; port <= endPort; port++) {
            final int p = port;
            executor.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(ip, p), 200);
                    openPorts.add(p);
                    SwingUtilities.invokeLater(() -> console.print("Открыт порт: " + p));
                } catch (Exception ignored) {
                    SwingUtilities.invokeLater(() -> console.print("Порт " + p + " закрыт"));
                }
            });
        }

        executor.shutdown();

        new Thread(() -> {
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            SwingUtilities.invokeLater(() -> {
                console.print("============");
                console.print("Просканировано портов: " + (endPort - startPort + 1));
                console.print("Открыто портов: " + openPorts.size());
                console.print("Открытые порты: " + openPorts);
                console.print("============");
            });
        }).start();
    }
}
