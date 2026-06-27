package server;

import service.AuthenticationService;
import service.ChatService;
import service.RoomService;
import util.DateUtil;

/**
 * Prints a formatted server status snapshot to stdout.
 *
 * Invoked on a background timer so operators can monitor
 * the server without interrupting the accept loop.
 */
public class ServerDashboard {

    private final AuthenticationService authService;
    private final ChatService           chatService;
    private final RoomService           roomService;
    private final int                   port;
    private final long                  startTime;

    public ServerDashboard(AuthenticationService authService,
                           ChatService chatService,
                           RoomService roomService,
                           int port) {
        this.authService = authService;
        this.chatService = chatService;
        this.roomService = roomService;
        this.port        = port;
        this.startTime   = System.currentTimeMillis();
    }

    public void print() {
        long seconds = (System.currentTimeMillis() - startTime) / 1000;
        String uptime = String.format("%02d:%02d:%02d",
                seconds / 3600, (seconds % 3600) / 60, seconds % 60);

        System.out.println();
        System.out.println("================================");
        System.out.println("        SERVER DASHBOARD        ");
        System.out.println("================================");
        System.out.printf("  %-16s: %s%n", "Status",          "Running");
        System.out.printf("  %-16s: %d%n", "Port",            port);
        System.out.printf("  %-16s: %s%n", "Uptime",          uptime);
        System.out.printf("  %-16s: %d%n", "Connected Users", authService.getOnlineCount());
        System.out.printf("  %-16s: %d%n", "Active Rooms",    roomService.getRoomCount());
        System.out.printf("  %-16s: %d%n", "Messages Sent",   chatService.getMessageCount());
        System.out.printf("  %-16s: %s%n", "Timestamp",       DateUtil.getCurrentDateTime());
        System.out.println("================================");
        System.out.println();
    }
}
