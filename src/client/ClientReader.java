package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Dedicated read thread for the client side.
 *
 * Blocks on the socket input stream and prints every server message
 * to stdout.  Runs as a daemon thread so it doesn't block JVM exit
 * after the user quits.
 *
 * Triggers System.exit(0) on connection loss so the writer thread
 * (blocked on System.in) is also terminated cleanly.
 */
public class ClientReader implements Runnable {

    private final Socket         socket;
    private volatile boolean     running = true;

    public ClientReader(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String line;
            while (running && (line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("\n[CLIENT] Connection to server lost: " + e.getMessage());
            }
        } finally {
            running = false;
            System.out.println("\n[CLIENT] Disconnected from server.");
            System.exit(0);
        }
    }

    public void stop() { running = false; }
}
