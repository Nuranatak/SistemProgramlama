import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String LOCALHOST = "localhost";
    private static final int SERVER1_PORT = 5001;
    private static final int SERVER2_PORT = 5002;
    private static final int SERVER3_PORT = 5003;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("""
                    Hangi sunucuya mesaj göndermek istiyorsunuz?
                    (1: SERVER1, 2: SERVER2, 3: SERVER3) ya da çıkmak için 'exit' yazın:
                    """);
            String serverChoice = scanner.nextLine();

            if (serverChoice.equalsIgnoreCase("exit")) {
                System.out.println("Çıkış yapılıyor...");
                break;
            }

            int port = getServerPort(serverChoice);
            if (port == -1) {
                System.out.println("Geçersiz sunucu seçimi. Lütfen tekrar deneyin.");
                continue;
            }

            System.out.println("""
                    Komut girin (SUBS, DEL):
                    - SUBS: Abone olmak için
                    - DEL: Abonelikten çıkmak için
                    """);
            String command = scanner.nextLine();

            switch (command.toUpperCase()) {
                case "SUBS" -> handleSubscription(scanner, LOCALHOST, port);
                case "DEL" -> handleDeletion(scanner, LOCALHOST, port);
                default -> System.out.println("Bilinmeyen komut. Lütfen geçerli bir komut girin.");
            }
        }

        scanner.close();
    }

    private static int getServerPort(String serverChoice) {
        return switch (serverChoice) {
            case "1" -> SERVER1_PORT;
            case "2" -> SERVER2_PORT;
            case "3" -> SERVER3_PORT;
            default -> -1;
        };
    }

    private static void handleSubscription(Scanner scanner, String host, int port) {
        try {
            System.out.print("Abone ID'sini girin: ");
            int id = Integer.parseInt(scanner.nextLine());

            System.out.print("Abone adını girin: ");
            String name = scanner.nextLine();

            String message = String.format("SUBS,%d,%s", id, name);
            sendAndReceiveMessage(host, port, message);
        } catch (NumberFormatException e) {
            System.out.println("Geçersiz ID formatı. Lütfen geçerli bir sayı girin.");
        }
    }

    private static void handleDeletion(Scanner scanner, String host, int port) {
        try {
            System.out.print("Silmek istediğiniz abonenin ID'sini girin: ");
            int id = Integer.parseInt(scanner.nextLine());

            String message = String.format("DEL,%d", id);
            sendAndReceiveMessage(host, port, message);
        } catch (NumberFormatException e) {
            System.out.println("Geçersiz ID formatı. Lütfen geçerli bir sayı girin.");
        }
    }

    private static void sendAndReceiveMessage(String host, int port, String message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(message);
            String response = in.readLine();
            System.out.println("Port " + port + " üzerinden sunucudan gelen yanıt: " + response);

        } catch (IOException e) {
            System.out.println("Port " + port + " üzerindeki sunucuya bağlanırken hata oluştu: " + e.getMessage());
        }
    }
}
