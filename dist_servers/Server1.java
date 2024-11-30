package java_servers;

import java.io.*;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;

public class Server1 {
    private static final int PORT = 5001;
    private static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server1 is running on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
             
            String message = in.readLine();
            System.out.println("Received: " + message);

            if (message.contains("demand=STRT")) {
                lock.lock();
                try {
                    out.println("response=YEP");
                } finally {
                    lock.unlock();
                }
            } else {
                out.println("response=NOP");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
