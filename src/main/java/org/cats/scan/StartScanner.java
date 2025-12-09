package org.cats.scan;

import org.cats.gui.ConsolePanel;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

public class StartScanner {

    private final int threads;
    private final Map<Integer, String> protocolVersions;

    public StartScanner(int threads) {
        this.threads = threads;
        this.protocolVersions = loadProtocolVersions();
    }

    private Map<Integer, String> loadProtocolVersions() {
        Map<Integer, String> versions = new ConcurrentHashMap<>();

        try {
            URL url = new URL("https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/common/protocolVersions.json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();

            int start = 0;
            while (true) {
                int versionIdIndex = json.indexOf("\"version\"", start);
                if (versionIdIndex == -1) break;

                int protocolIndex = json.indexOf("\"protocol\"", versionIdIndex);
                if (protocolIndex == -1) break;

                int versionStart = json.indexOf("\"", versionIdIndex + 9) + 1;
                int versionEnd = json.indexOf("\"", versionStart);
                String versionName = json.substring(versionStart, versionEnd);

                int protocolStart = json.indexOf(":", protocolIndex) + 1;
                int protocolEnd = json.indexOf(",", protocolStart);
                if (protocolEnd == -1) protocolEnd = json.indexOf("}", protocolStart);

                String protocolStr = json.substring(protocolStart, protocolEnd).trim();
                try {
                    int protocol = Integer.parseInt(protocolStr);

                    if (versionName.matches("1\\.\\d+(\\.\\d+)?") && protocol > 0) {
                        versions.put(protocol, versionName);
                    }
                } catch (NumberFormatException ignored) {
                }

                start = protocolEnd;
            }

        } catch (Exception ignored) {
        }

        if (versions.isEmpty()) {
            versions.put(766, "1.20.4");
            versions.put(765, "1.20.3");
            versions.put(764, "1.20.2");
            versions.put(763, "1.20.1");
            versions.put(762, "1.19.4");
            versions.put(761, "1.19.3");
            versions.put(760, "1.19.2");
            versions.put(759, "1.19");
            versions.put(758, "1.18.2");
            versions.put(757, "1.18.1");
            versions.put(756, "1.17.1");
            versions.put(755, "1.17");
            versions.put(754, "1.16.5");
            versions.put(753, "1.16.3");
            versions.put(751, "1.16.2");
            versions.put(736, "1.16.1");
            versions.put(735, "1.16");
            versions.put(578, "1.15.2");
            versions.put(575, "1.15.1");
            versions.put(573, "1.15");
            versions.put(498, "1.14.4");
            versions.put(404, "1.13.2");
            versions.put(340, "1.12.2");
            versions.put(316, "1.11.2");
            versions.put(210, "1.10.2");
            versions.put(110, "1.9.4");
            versions.put(109, "1.9.2");
            versions.put(107, "1.9");
            versions.put(47, "1.8.x");
        }

        return versions;
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int size = 0;
        int b;
        while (((b = in.readByte()) & 0x80) == 0x80) {
            value |= (b & 0x7F) << (size++ * 7);
            if (size > 5) throw new IOException("VarInt too long");
        }
        return value | (b & 0x7F) << (size * 7);
    }

    private String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private boolean isMinecraftServer(String ip, int port) {
        List<Integer> protocolList = new ArrayList<>(protocolVersions.keySet());
        protocolList.sort(Collections.reverseOrder());

        for (int protocolVersion : protocolList) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), 1500);
                socket.setSoTimeout(2000);

                OutputStream rawOut = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(rawOut);
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream handshake = new DataOutputStream(baos);

                handshake.writeByte(0x00);
                writeVarInt(handshake, protocolVersion);
                writeString(handshake, ip);
                handshake.writeShort(port);
                writeVarInt(handshake, 1);

                byte[] handshakeData = baos.toByteArray();
                writeVarInt(dos, handshakeData.length);
                dos.write(handshakeData);
                dos.flush();

                dos.writeByte(0x01);
                dos.writeByte(0x00);
                dos.flush();

                int responseLength = readVarInt(dis);
                if (responseLength > 0) {
                    int responsePacketId = readVarInt(dis);
                    if (responsePacketId == 0x00) {
                        readString(dis);
                        return true;
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }

        return false;
    }

    public void scan(String ip, int startPort, int endPort, ConsolePanel console) {
        console.print("Загружено протоколов: " + protocolVersions.size());
        console.print("Старт сканирования " + ip + " (" + startPort + "-" + endPort + ") потоков: " + threads);

        List<Integer> openPorts = Collections.synchronizedList(new ArrayList<>());
        List<Integer> minecraftPorts = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int port = startPort; port <= endPort; port++) {
            final int p = port;
            executor.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(ip, p), 1500);
                    openPorts.add(p);

                    SwingUtilities.invokeLater(() -> console.print("Порт " + p + " открыт, проверяем на Minecraft..."));

                    boolean isMinecraft = isMinecraftServer(ip, p);

                    if (isMinecraft) {
                        minecraftPorts.add(p);
                        SwingUtilities.invokeLater(() -> console.print("Minecraft сервер найден на порту: " + p));
                    } else {
                        try (Socket testSocket = new Socket()) {
                            testSocket.connect(new InetSocketAddress(ip, p), 1000);
                            testSocket.setSoTimeout(1000);

                            OutputStream out = testSocket.getOutputStream();
                            DataOutputStream dos = new DataOutputStream(out);

                            dos.writeByte(0xFE);
                            dos.writeByte(0x01);
                            dos.writeByte(0xFA);
                            dos.writeShort(11);
                            dos.write("MC|PingHost".getBytes(StandardCharsets.UTF_16BE));
                            dos.writeShort(7 + 2 * ip.length());
                            dos.writeByte(73);
                            dos.writeShort(ip.length());
                            dos.write(ip.getBytes(StandardCharsets.UTF_16BE));
                            dos.writeInt(p);
                            dos.flush();

                            DataInputStream dis = new DataInputStream(testSocket.getInputStream());
                            int response = dis.readUnsignedByte();
                            if (response == 0xFF) {
                                minecraftPorts.add(p);
                                SwingUtilities.invokeLater(() -> console.print("Legacy Minecraft сервер найден на порту: " + p));
                            }
                        } catch (Exception e) {
                            SwingUtilities.invokeLater(() -> console.print("Порт " + p + " открыт (не Minecraft)"));
                        }
                    }

                } catch (SocketTimeoutException e) {
                    SwingUtilities.invokeLater(() -> console.print("Порт " + p + " недоступен (таймаут)"));
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> console.print("Порт " + p + " закрыт"));
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> console.print("Порт " + p + " ошибка: " + e.getClass().getSimpleName()));
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
                console.print("Всего открытых портов: " + openPorts.size());
                console.print("Найдено Minecraft серверов: " + minecraftPorts.size());
                console.print("Minecraft порты: " + minecraftPorts);
                if (minecraftPorts.size() < openPorts.size()) {
                    console.print("Другие открытые порты: " + openPorts);
                }
                console.print("============");
            });
        }).start();
    }
}