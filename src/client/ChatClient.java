package client;

import util.Constants;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Entry point for the chat client.
 *
 * Establishes a TCP connection to the server, then starts two threads:
 *   ClientReader  – receives and displays messages
 *   ClientWriter  – reads keyboard input and sends to server
 *
 * The main thread blocks on the writer thread (which exits on /quit
 * or EOF on stdin) and then initiates a clean shutdown.
 *
 * Usage:
 *   java client.ChatClient [host] [port]
 *   java client.ChatClient                   # uses localhost:6000
 *   java client.ChatClient 192.168.1.10 6000
 */
public class ChatClient {

    private final String host;
    private final int    port;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() {
        System.out.println("================================");
        System.out.println("   Real-Time Chat Client v1.0  ");
        System.out.println("================================");
        System.out.println("  Server : " + host + ":" + port);
        System.out.println("  Connecting...\n");

        try (Socket socket = new Socket(host, port)) {
            System.out.println("  Connected!  Start typing.\n");

            ClientReader reader = new ClientReader(socket);
            ClientWriter writer = new ClientWriter(socket);

            Thread readerThread = new Thread(reader, "Reader");
            Thread writerThread = new Thread(writer, "Writer");

            readerThread.setDaemon(true);  // exits when JVM exits
            readerThread.start();
            writerThread.start();

            writerThread.join();  // block until user types /quit or EOF
            reader.stop();

        } catch (UnknownHostException e) {
            System.err.println("[CLIENT] Host not found: " + host);
        } catch (IOException e) {
            System.err.println("[CLIENT] Could not connect to " + host + ":" + port);
            System.err.println("         Is the server running?");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("[CLIENT] Session ended. Goodbye!");
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        String host = Constants.SERVER_HOST;
        int    port = Constants.DEFAULT_PORT;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port '" + args[1]
                        + "'. Using default: " + Constants.DEFAULT_PORT);
            }
        }

        new ChatClient(host, port).connect();
    }
}
