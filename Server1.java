import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Server1 {
    private static final int PORT = 5001;
    private static final int ADMIN_PORT = 6001;
    private static final int BACKUP_PORT = 7001;
    private static volatile boolean running = true;
    private static Map<Integer, Subscriber> subscribers = new HashMap<>();
    private static volatile int faultToleranceLevel = 0;
    private static String[] backupServers = { "localhost:7002", "localhost:7003" };

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);

        new AdminListenerThread(ADMIN_PORT).start();

        new PingThread("localhost", 5002).start();
        new PingThread("localhost", 5003).start();

        new BackupListenerThread(BACKUP_PORT).start();

        try {
            while (running) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    private static class BackupListenerThread extends Thread {
        private final int backupPort;

        public BackupListenerThread(int backupPort) {
            this.backupPort = backupPort;
        }

        public void run() {
            try (ServerSocket backupSocket = new ServerSocket(backupPort)) {
                System.out.println("Yedek port dinleniyor: " + backupPort);

                while (true) {
                    try (Socket backupClientSocket = backupSocket.accept();
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(backupClientSocket.getInputStream()))) {

                        String message = in.readLine();
                        System.out.println("Yedek sunucudan gelen mesaj: " + message);

                        // Gelen mesajı işle
                        if (message != null && !message.isEmpty()) {
                            processBackupMessage(message);
                        }

                    } catch (IOException e) {
                        System.err.println("Yedek işlem hatası: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Backup portu açılırken hata: " + e.getMessage());
            }
        }

        private void processBackupMessage(String message) {
            if (message == null || message.isEmpty()) {
                return;
            }

            if (message.contains(",fromServer1")) {
                System.out.println("Server1'den gelen mesaj alındı, ancak geri dönüş yapılmayacak.");
                return;
            }

            String[] parts = message.split(",");
            String command = parts[0];

            switch (command) {
                case "SUBS":
                    int id = Integer.parseInt(parts[1]);
                    String nameSurname = parts[2];
                    Subscriber newSubscriber = new Subscriber(id, nameSurname, System.currentTimeMillis(),
                            System.currentTimeMillis(), new String[] {}, true);
                    subscribers.put(id, newSubscriber);
                    System.out.println("Yedek sunucudan abone eklendi: " + nameSurname);
                    break;

                case "DEL":
                    int idToRemove = Integer.parseInt(parts[1]);
                    subscribers.remove(idToRemove);
                    System.out.println("Yedek sunucudan abone silindi: " + idToRemove);
                    break;

                default:
                    System.err.println("Bilinmeyen yedek komutu: " + command);
            }
        }

    }

    private static class AdminListenerThread extends Thread {
        private final int adminPort;

        public AdminListenerThread(int adminPort) {
            this.adminPort = adminPort;
        }

        public void run() {
            try (ServerSocket adminSocket = new ServerSocket(adminPort)) {
                System.out.println("Admin port dinleniyor: " + adminPort);
                adminSocket.setSoTimeout(10000);

                while (true) {
                    try (Socket adminClientSocket = adminSocket.accept();
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(adminClientSocket.getInputStream()))) {

                        String message = in.readLine();
                        if (message == null) {
                            System.out.println("Bağlantı kapatıldı veya boş mesaj alındı.");
                            continue;
                        }

                        System.out.println("Admin'den gelen mesaj: " + message);

                        if (message.startsWith("FAULT_TOLERANCE:")) {
                            String[] parts = message.split(":");
                            if (parts.length == 2) {
                                try {
                                    faultToleranceLevel = Integer.parseInt(parts[1].trim());
                                    System.out.println("Güncellenen fault_tolerance_level: " + faultToleranceLevel);
                                } catch (NumberFormatException e) {
                                    System.err.println("Hatalı fault_tolerance_level formatı: " + parts[1]);
                                }
                            } else {
                                System.err.println("FAULT_TOLERANCE mesajı hatalı formatta.");
                            }
                        }

                    } catch (SocketTimeoutException e) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } catch (IOException e) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Admin portu açılırken hata: " + e.getMessage());
            }
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String message = in.readLine();
                Message responseMessage = processMessage(message);

                out.println(responseMessageToString(responseMessage));
            } catch (IOException e) {
                System.err.println("Error handling client request: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Failed to close client socket: " + e.getMessage());
                }
            }
        }

        private Message processMessage(String message) {
            if (message == null || message.isEmpty()) {
                return null;
            }

            String[] parts = message.split(",");
            String command = parts[0];

            switch (command) {
                case "SUBS":
                    Subscriber newSubscriber = new Subscriber(Integer.parseInt(parts[1]), parts[2],
                            System.currentTimeMillis(), System.currentTimeMillis(), new String[] {}, true);
                    addSubscriber(newSubscriber);
                    updateCapacity();
                    if (faultToleranceLevel == 1) {
                        sendToBackupServers("SUBS," + parts[1] + "," + parts[2], 1);
                    } else if (faultToleranceLevel == 2) {
                        sendToBackupServers("SUBS," + parts[1] + "," + parts[2], 2);
                    }
                    return new Message(Message.Demand.SUBS, newSubscriber, "YEP");

                case "DEL":
                    int idToRemove = Integer.parseInt(parts[1]);
                    if (subscribers.containsKey(idToRemove)) {
                        removeSubscriber(idToRemove);
                        updateCapacity();
                        if (faultToleranceLevel == 1) {
                            sendToBackupServers("DEL," + parts[1], 1);
                        } else if (faultToleranceLevel == 2) {
                            sendToBackupServers("DEL," + parts[1], 2);
                        }
                        return new Message(Message.Demand.DEL, new Subscriber(idToRemove, null, 0, 0, null, false),
                                "YEP");
                    } else {
                        return new Message(Message.Demand.DEL, null, "NO");
                    }

                case "CPCTY":
                    return new Message(Message.Demand.CPCTY, null, "YEP");

                case "STRT":
                    return new Message(Message.Demand.STRT, null, "YEP");

                default:
                    return null;
            }
        }

        private void addSubscriber(Subscriber subscriber) {
            subscribers.put(subscriber.getId(), subscriber);
        }

        private void removeSubscriber(int id) {
            subscribers.remove(id);
        }

        private void updateCapacity() {
            int capacity = getDynamicCapacity();
            System.out.println("Güncellenen kapasite: " + capacity + "%");
            sendToPlotter("Server1:" + capacity);
        }

        private String responseMessageToString(Message responseMessage) {
            if (responseMessage == null)
                return "Unknown command received.";

            switch (responseMessage.getDemand()) {
                case SUBS:
                    return "Subscriber added: " + responseMessage.getSubscriber().getNameSurname() + " Response: "
                            + responseMessage.getResponse();
                case DEL:
                    return "Subscriber removed: "
                            + (responseMessage.getSubscriber() != null ? responseMessage.getSubscriber().getId()
                                    : "Unknown ID")
                            + " Response: " + responseMessage.getResponse();
                case CPCTY:
                    int status = getDynamicCapacity();
                    sendToPlotter("Server1:" + status);
                    return "server1_status:" + status + " Response: " + responseMessage.getResponse();
                case STRT:
                    return "Server1 started. Response: " + responseMessage.getResponse();
                default:
                    return "Unknown response.";
            }
        }

        private int getDynamicCapacity() {
            int maxCapacity = 100;
            int currentSubscribers = subscribers.size();
            int capacity = (int) (((double) currentSubscribers / maxCapacity) * 100);

            return Math.min(capacity, 100);
        }

        private void sendToPlotter(String data) {
            try (Socket plotterSocket = new Socket("localhost", 7000)) {
                OutputStream out = plotterSocket.getOutputStream();
                out.write((data + "\n").getBytes("UTF-8"));
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending data to plotter: " + e.getMessage());
            }
        }

        private void sendToBackupServers(String message, int toleranceLevel) {
            int limit = (toleranceLevel == 1) ? 1 : backupServers.length;

            for (int i = 0; i < limit; i++) {
                String[] addressParts = backupServers[i].split(":");
                String host = addressParts[0];
                int port = Integer.parseInt(addressParts[1]);

                try (Socket backupSocket = new Socket(host, port);
                        PrintWriter out = new PrintWriter(backupSocket.getOutputStream(), true)) {

                    String messageToSend = message + ",fromServer1";
                    out.println(messageToSend);
                    System.out.println("Yedek sunucuya gönderilen mesaj: " + messageToSend);
                } catch (IOException e) {
                    System.err.println("Yedek sunucuya veri gönderilirken hata oluştu (" + host + ":" + port + "): "
                            + e.getMessage());
                }
            }
        }
    }

    static class PingThread extends Thread {
        private final String host;
        private final int port;

        public PingThread(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public void run() {
            while (!isInterrupted()) {
                try (Socket socket = new Socket(host, port)) {
                    System.out.println("Pinged " + host + " on port " + port);
                } catch (IOException e) {
                    System.out.println("Ping to " + host + " on port " + port + " failed, retrying...");
                }

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {
                    System.out.println("Ping thread interrupted: " + ie.getMessage());
                    break;
                }
            }
        }
    }
}