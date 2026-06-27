package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Dedicated write thread for the client side.
 *
 * Reads lines from System.in (the keyboard) and forwards them to the server.
 * When the user types /quit the loop exits and the main thread can close
 * the socket.
 */
public class ClientWriter implements Runnable {

    private final Socket         socket;
    private volatile boolean     running = true;

    public ClientWriter(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (PrintWriter writer  = new PrintWriter(socket.getOutputStream(), true);
             Scanner     scanner = new Scanner(System.in)) {

            while (running && scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                writer.println(line);

                if (line.equalsIgnoreCase("/quit")) {
                    running = false;
                    break;
                }
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("[CLIENT] Send error: " + e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    public void stop() { running = false; }
}
