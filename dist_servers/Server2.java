package java_servers;

import java.io.*;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;

public class Server2 {
    private static final int PORT = 5002; // Port numarası
    private static final ReentrantLock lock = new ReentrantLock(); // Thread-safe için kilit

    public static void main(String[] args) {
        // Diğer sunuculara heartbeat gönderimi için iş parçacıkları başlat
        new Thread(new HeartbeatSender("localhost", 5001)).start();
        new Thread(new HeartbeatSender("localhost", 5003)).start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server1 is running on port " + PORT);

            while (true) {
                // Gelen bağlantıyı kabul et ve iş parçacığında işle
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting the server: " + e.getMessage());
        }
    }

    // İstemci bağlantılarını işleyen metot
    private static void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String message = in.readLine();
            System.out.println("Received: " + message);

            // Mesaj işleme
            if (message != null && message.startsWith("demand=")) {
                String demand = message.split("=")[1];

                switch (demand) {
                    case "STRT":
                        sendResponse(out, "YEP");
                        break;
                    case "CPCTY":
                        sendCapacityResponse(out);
                        break;
                    default:
                        sendResponse(out, "NOP");
                        break;
                }
            } else if (message != null && message.startsWith("HEARTBEAT")) {
                // Heartbeat mesajını işle
                System.out.println("Received heartbeat from: " + message.split(";")[1]);
                sendResponse(out, "HEARTBEAT_ACK");
            } else {
                sendResponse(out, "NOP");
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    // Yanıt gönderme metodu
    private static void sendResponse(PrintWriter out, String response) {
        lock.lock();
        try {
            out.println("response=" + response);
        } finally {
            lock.unlock();
        }
    }

    // Kapasite bilgisi gönderme metodu
    private static void sendCapacityResponse(PrintWriter out) {
        lock.lock();
        try {
            long timestamp = System.currentTimeMillis() / 1000L; // Unix zaman damgası
            int serverStatus = 1000; // Örnek kapasite bilgisi
            out.println("response=CPCTY;server1_status=" + serverStatus + ";timestamp=" + timestamp);
        } finally {
            lock.unlock();
        }
    }

    // Heartbeat göndermek için iç sınıf
    static class HeartbeatSender implements Runnable {
        private String targetHost;
        private int targetPort;

        public HeartbeatSender(String targetHost, int targetPort) {
            this.targetHost = targetHost;
            this.targetPort = targetPort;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Socket socket = new Socket(targetHost, targetPort);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    // Heartbeat mesajını gönder
                    out.println("HEARTBEAT;Server1");
                    socket.close();

                    // 5 saniyede bir kontrol et
                    Thread.sleep(5000);
                } catch (Exception e) {
                    System.out.println("Failed to connect to " + targetHost + ":" + targetPort);
                }
            }
        }
    }
}
