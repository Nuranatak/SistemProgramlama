package SistemProgramlama.dist_servers;

import java.io.*;
import java.net.*;
import java.time.Instant;

import SistemProgramlama.dist_servers.CapacityProto.Capacity;
import SistemProgramlama.dist_servers.ConfigurationProto.Configuration;
import SistemProgramlama.dist_servers.MessageProto.Message;

public class Server3 {
    public static void main(String[] args) {
        int port = 5003;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server3 started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Admin connected: " + clientSocket);

                InputStream input = clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream();

                // Message nesnesini al ve isteğe göre işlem yap
                Message message = Message.parseFrom(input);

                if (message.getDemand() == Message.Demand.CPCTY) {
                    // Kapasite yanıtı oluştur
                    int serverStatus = 1000; // Örnek kapasite durumu
                    long timestamp = Instant.now().getEpochSecond();

                    Capacity capacity = Capacity.newBuilder()
                                                .setServerStatus(serverStatus)
                                                .setTimestamp(timestamp)
                                                .build();

                    output.write(capacity.toByteArray());
                } else {
                    System.out.println("Unknown request received");
                }

                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

