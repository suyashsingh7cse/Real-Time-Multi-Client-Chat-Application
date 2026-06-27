package server;

import service.AuthenticationService;
import service.ChatService;
import service.HistoryService;
import service.RoomService;
import util.Constants;
import util.LoggerUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entry point and orchestrator for the chat server.
 *
 * Architecture:
 *   - One thread blocks on ServerSocket.accept()
 *   - Each accepted connection is submitted to a cached thread pool
 *   - A background daemon timer prints the ServerDashboard every 30 s
 *
 * All service singletons are created here and injected into handlers,
 * keeping service classes free of static state (testable, replaceable).
 */
public class ChatServer {

    private final int                        port;
    private final AuthenticationService      authService;
    private final ChatService                chatService;
    private final RoomService                roomService;
    private final HistoryService             historyService;
    private final Map<String, ClientHandler> handlers;
    private final ExecutorService            threadPool;
    private final ServerDashboard            dashboard;
    private volatile boolean                 running = false;

    public ChatServer(int port) {
        this.port           = port;
        this.historyService = new HistoryService();
        this.authService    = new AuthenticationService();
        this.roomService    = new RoomService();
        this.chatService    = new ChatService(historyService);
        this.handlers       = new ConcurrentHashMap<>();
        this.threadPool     = Executors.newCachedThreadPool();
        this.dashboard      = new ServerDashboard(authService, chatService, roomService, port);
    }

    // ── Startup ───────────────────────────────────────────────────────────────

    public void start() {
        running = true;
        printBanner();
        scheduleDashboard();
        LoggerUtil.logServer("Server starting on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    LoggerUtil.logServer("Incoming connection from "
                            + client.getInetAddress().getHostAddress());

                    ClientHandler handler = new ClientHandler(
                            client, authService, chatService,
                            roomService, historyService, handlers);

                    threadPool.execute(handler);
                } catch (IOException e) {
                    if (running) LoggerUtil.logError("Accept error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            LoggerUtil.logError("Server socket failure: " + e.getMessage());
            System.err.println("[FATAL] Could not bind to port " + port + ": " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void scheduleDashboard() {
        Timer timer = new Timer("DashboardTimer", true); // daemon – won't block JVM exit
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { dashboard.print(); }
        }, Constants.DASHBOARD_DELAY, Constants.DASHBOARD_INTERVAL);
    }

    private void shutdown() {
        running = false;
        threadPool.shutdown();
        LoggerUtil.logServer("Server shut down cleanly.");
        System.out.println("[SERVER] Shut down.");
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    private void printBanner() {
        System.out.println("================================");
        System.out.println("   Real-Time Chat Server v1.0  ");
        System.out.println("================================");
        System.out.println("  Port    : " + port);
        System.out.println("  Status  : Starting...");
        System.out.println("================================");
        System.out.println("  Waiting for connections...\n");
    }

    public static void main(String[] args) {
        int port = Constants.DEFAULT_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port '" + args[0]
                        + "'. Using default: " + Constants.DEFAULT_PORT);
            }
        }

        new ChatServer(port).start();
    }
}
