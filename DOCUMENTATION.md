# DOCUMENTATION — Real-Time Multi-Client Chat Application

---

## Abstract

This document provides a comprehensive technical reference for a Real-Time
Multi-Client Chat Application built using pure Java 17. The system demonstrates
professional-grade software engineering: layered architecture, concurrent
socket programming, persistent storage, and structured logging — all without
external libraries or frameworks.

---

## Problem Statement

Modern communication systems require servers capable of handling many simultaneous
users with low latency, reliable message delivery, and persistent history. This
project solves the problem at the foundational level by implementing a TCP-based
chat server from scratch, exposing every design decision that production frameworks
typically abstract away.

---

## Objectives

1. Demonstrate Java socket programming via a multi-client server
2. Apply multithreading to handle N concurrent connections independently
3. Implement persistent message history using file I/O
4. Design clean, layered architecture following SOLID principles
5. Provide a complete command system with robust error handling
6. Produce structured logs across three dedicated channels

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT SIDE                          │
│                                                             │
│  ┌──────────────┐    ┌───────────────┐  ┌───────────────┐  │
│  │  ChatClient  │───▶│  ClientWriter  │  │ ClientReader  │  │
│  │  (main)      │    │  (kbd → socket)│  │(socket→stdout)│  │
│  └──────────────┘    └───────────────┘  └───────────────┘  │
│           │                 │                    ▲           │
└───────────┼─────────────────┼────────────────────┼──────────┘
            │           TCP Socket              TCP Socket
┌───────────┼─────────────────┼────────────────────┼──────────┐
│           │          SERVER SIDE                  │          │
│  ┌────────▼──────┐   ┌──────▼──────────────────────────┐    │
│  │  ChatServer   │──▶│         ClientHandler            │    │
│  │  accept loop  │   │   (one thread per connection)    │    │
│  └───────────────┘   └──────────────────────────────────┘    │
│         │                        │                           │
│  ┌──────▼───────┐    ┌───────────┼──────────────────────┐    │
│  │ServerDashboard│   │           ▼  SERVICE LAYER        │    │
│  │  (timer)     │   │  AuthService  ChatService          │    │
│  └──────────────┘   │  RoomService  HistoryService       │    │
│                      └───────────┬──────────────────────┘    │
│                                  │                           │
│                         ┌────────▼───────────┐              │
│                         │    DATA LAYER       │              │
│                         │  data/history/*.txt │              │
│                         │  data/rooms/*.txt   │              │
│                         │  logs/*.log         │              │
│                         └─────────────────────┘              │
└─────────────────────────────────────────────────────────────┘
```

---

## Class Diagram (ASCII UML)

```
┌──────────────────────────────────────────────────────────────────┐
│  MODEL                                                           │
│                                                                  │
│  ┌──────────────┐    ┌───────────────────┐    ┌──────────────┐  │
│  │    User      │    │     Message       │    │  ChatRoom    │  │
│  │─────────────│    │───────────────────│    │──────────────│  │
│  │ -username    │    │ -sender: String   │    │ -name        │  │
│  │ -joinTime    │    │ -content: String  │    │ -createdBy   │  │
│  │ -online      │    │ -timestamp: String│    │ -members     │  │
│  │─────────────│    │ -type: Type{enum} │    │ -msgCache    │  │
│  │ +getUsername │    │ -recipient:String │    │──────────────│  │
│  │ +isOnline    │    │───────────────────│    │ +addMember   │  │
│  │ +setOnline   │    │ +getSender()      │    │ +hasMember   │  │
│  └──────────────┘    │ +getContent()     │    │ +getMembers  │  │
│                      │ +getType()        │    └──────────────┘  │
│                      └───────────────────┘                      │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  SERVICE                                                         │
│                                                                  │
│  ┌─────────────────────┐    ┌──────────────────────────────┐    │
│  │ AuthenticationService│    │       ChatService            │    │
│  │─────────────────────│    │──────────────────────────────│    │
│  │ -onlineUsers: Map   │    │ -historyService              │    │
│  │─────────────────────│    │ -messageCount: AtomicLong    │    │
│  │ +registerUser(User) │    │──────────────────────────────│    │
│  │ +removeUser(String) │    │ +createBroadcastMessage()    │    │
│  │ +isUserOnline()     │    │ +createPrivateMessage()      │    │
│  │ +getOnlineUsers()   │    │ +createRoomMessage()         │    │
│  └─────────────────────┘    │ +createSystemMessage()       │    │
│                              └──────────────────────────────┘    │
│  ┌─────────────────────┐    ┌──────────────────────────────┐    │
│  │    RoomService      │    │      HistoryService           │    │
│  │─────────────────────│    │──────────────────────────────│    │
│  │ -rooms: Map         │    │ -writeLock: Object           │    │
│  │─────────────────────│    │──────────────────────────────│    │
│  │ +createRoom()       │    │ +saveGeneralMessage()        │    │
│  │ +joinRoom()         │    │ +savePrivateMessage()        │    │
│  │ +leaveRoom()        │    │ +saveRoomMessage()           │    │
│  │ +getAllRooms()       │    │ +loadGeneralHistory()        │    │
│  └─────────────────────┘    │ +loadPrivateHistory()        │    │
│                              │ +loadRoomHistory()           │    │
│                              └──────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  SERVER                                                          │
│                                                                  │
│  ┌─────────────────────┐    ┌──────────────────────────────┐    │
│  │     ChatServer      │◀───│       ClientHandler          │    │
│  │─────────────────────│    │──────────────────────────────│    │
│  │ -port: int          │    │ -socket: Socket              │    │
│  │ -handlers: Map      │    │ -username: String            │    │
│  │ -threadPool         │    │ -reader: BufferedReader      │    │
│  │─────────────────────│    │ -writer: PrintWriter         │    │
│  │ +start()            │    │──────────────────────────────│    │
│  │ +main()             │    │ +run()                       │    │
│  └─────────────────────┘    │ +send(String)                │    │
│                              │ -authenticate()              │    │
│  ┌─────────────────────┐    │ -handleCommand()             │    │
│  │   ServerDashboard   │    │ -handleBroadcast()           │    │
│  │─────────────────────│    │ -handleRoomMessage()         │    │
│  │ +print()            │    └──────────────────────────────┘    │
│  └─────────────────────┘                                        │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  CLIENT                                                          │
│                                                                  │
│  ┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │  ChatClient  │  │   ClientReader   │  │  ClientWriter    │   │
│  │──────────────│  │──────────────────│  │──────────────────│   │
│  │ +connect()   │  │ +run()           │  │ +run()           │   │
│  │ +main()      │  │ +stop()          │  │ +stop()          │   │
│  └──────────────┘  └──────────────────┘  └──────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Sequence Diagram — User Login and Broadcast

```
Client              ChatServer          ClientHandler       AuthService
  │                     │                    │                  │
  │──connect()─────────▶│                    │                  │
  │                     │──new Handler()────▶│                  │
  │                     │──threadPool.exec──▶│                  │
  │                     │                    │──send(banner)───▶│
  │◀──────────(banner)──│                    │                  │
  │──"Suyash"──────────▶│                    │                  │
  │                     │────────────────────│──registerUser()─▶│
  │                     │                    │◀──true───────────│
  │◀──"Welcome, Suyash!"│                    │                  │
  │                     │                    │                  │
  │──"Hello everyone"──▶│                    │                  │
  │                     │────────────────────▶──createBroadcast │
  │                     │         for each handler:             │
  │◀──[23:12:11] Suyash: Hello everyone                        │
  (all other clients also receive the same formatted message)
```

---

## Sequence Diagram — Private Message

```
Alice             ClientHandler(Alice)         ClientHandler(Bob)        Bob
  │                      │                            │                   │
  │──/msg Bob Hi!───────▶│                            │                   │
  │                      │──createPrivateMessage()    │                   │
  │                      │──savePrivateMessage()      │                   │
  │◀─[PM → Bob] Hi!──────│                            │                   │
  │                      │──handlers.get("Bob")──────▶│                   │
  │                      │                            │──send([PM ← Alice] Hi!)▶│
```

---

## Use Case Diagram

```
                ┌────────────────────────────────────────┐
                │         Chat Application System        │
                │                                        │
  ┌──────┐      │  ┌─────────────────────────────────┐  │
  │      │──────│─▶│ Connect & Authenticate           │  │
  │      │      │  └─────────────────────────────────┘  │
  │      │      │  ┌─────────────────────────────────┐  │
  │      │──────│─▶│ Send Broadcast Message           │  │
  │      │      │  └─────────────────────────────────┘  │
  │      │      │  ┌─────────────────────────────────┐  │
  │      │──────│─▶│ Send Private Message (/msg)      │  │
  │      │      │  └─────────────────────────────────┘  │
  │ User │      │  ┌─────────────────────────────────┐  │
  │      │──────│─▶│ Create / Join / Leave Room       │  │
  │      │      │  └─────────────────────────────────┘  │
  │      │      │  ┌─────────────────────────────────┐  │
  │      │──────│─▶│ Send Room Message (@room)        │  │
  │      │      │  └─────────────────────────────────┘  │
  │      │      │  ┌─────────────────────────────────┐  │
  │      │──────│─▶│ View History (/history)          │  │
  │      │      │  └─────────────────────────────────┘  │
  │      │      │  ┌─────────────────────────────────┐  │
  │      │──────│─▶│ List Users / Rooms               │  │
  └──────┘      │  └─────────────────────────────────┘  │
                │                                        │
                │  ┌─────────────────────────────────┐  │
  ┌──────┐      │  │ View Server Dashboard            │  │
  │Admin │──────│─▶│ (auto-printed every 30 seconds)  │  │
  └──────┘      │  └─────────────────────────────────┘  │
                └────────────────────────────────────────┘
```

---

## Modules Description

### util Package

| Class | Responsibility |
|---|---|
| `Constants` | Single source of truth for all literals, paths, and command strings |
| `DateUtil` | Thread-safe timestamp formatting (no shared mutable state) |
| `LoggerUtil` | Synchronized file writer targeting three named log channels |

### model Package

| Class | Responsibility |
|---|---|
| `User` | Represents identity and presence of a connected user |
| `Message` | Immutable value object; carries type enum for routing |
| `ChatRoom` | Tracks membership and caches last 100 messages in memory |

### service Package

| Class | Responsibility |
|---|---|
| `AuthenticationService` | Username uniqueness, registration, removal |
| `ChatService` | Message factory; increments global counter; delegates to HistoryService |
| `RoomService` | Room lifecycle (create, join, leave); user-to-room reverse lookup |
| `HistoryService` | File-based persistence; alphabetical PM filename; `tail()` reader |

### server Package

| Class | Responsibility |
|---|---|
| `ChatServer` | Binds port, accepts connections, submits to cached thread pool |
| `ClientHandler` | Authentication handshake; read loop; command dispatch; graceful teardown |
| `ServerDashboard` | Periodic status summary printed to server stdout |

### client Package

| Class | Responsibility |
|---|---|
| `ChatClient` | Connects to server; wires reader/writer threads; manages lifecycle |
| `ClientReader` | Daemon thread: socket → stdout |
| `ClientWriter` | Foreground thread: stdin → socket |

---

## Thread Management

```
Main Thread (ChatServer)
  └── Blocks on ServerSocket.accept()
      ├── Per-client Thread (ClientHandler via ExecutorService)
      │     ├── Reads from socket (blocking)
      │     ├── Writes to other clients' sockets
      │     └── Exits on disconnect / /quit
      │
      └── Daemon Thread (DashboardTimer)
            └── Prints dashboard every 30 s

Main Thread (ChatClient)
  ├── Daemon Thread: ClientReader (socket → stdout)
  └── Foreground Thread: ClientWriter (stdin → socket)
        └── main.join() blocks until writer exits
```

**Thread Safety Mechanisms:**

| Resource | Mechanism |
|---|---|
| `handlers` map (String → ClientHandler) | `ConcurrentHashMap` |
| `onlineUsers` map | `ConcurrentHashMap` + `synchronized` on mutate |
| `rooms` map | `ConcurrentHashMap` + `synchronized` on create |
| Room membership | `CopyOnWriteArraySet` |
| Message counter | `AtomicLong` (lock-free CAS) |
| File writes | `synchronized(writeLock)` in HistoryService |
| Log file writes | `synchronized(FILE_LOCK)` in LoggerUtil |

---

## Socket Communication Flow

```
CLIENT                           SERVER
  │                                │
  │──TCP SYN──────────────────────▶│  ServerSocket.accept() unblocks
  │◀─TCP SYN-ACK───────────────────│  new Socket created
  │──TCP ACK──────────────────────▶│  ClientHandler spawned on thread pool
  │                                │
  │◀── banner + "Enter username:" ─│  PrintWriter(autoFlush=true)
  │── "Suyash\n" ─────────────────▶│  BufferedReader.readLine()
  │◀── "Welcome, Suyash!" ─────────│
  │                                │
  │── "Hello!\n" ─────────────────▶│  handleBroadcast()
  │◀── "[23:12:11] Suyash: Hello!" │  forwarded to all handlers
  │                                │
  │── "/quit\n" ───────────────────▶│  loop exits
  │                                │  teardown(): remove user, notify all
  │──TCP FIN──────────────────────▶│
  │◀─TCP FIN-ACK───────────────────│
```

---

## Data Storage Design

```
data/
├── history/
│   ├── general.txt           # All broadcast messages, append-only
│   │   Format: [HH:mm:ss] username: message
│   │
│   ├── pm_Alice_Bob.txt      # Private messages (names alphabetically sorted)
│   └── pm_Suyash_Rahul.txt
│
├── rooms/
│   ├── devs.txt              # Room messages for #devs
│   └── general.txt           # Room messages for #general
│
└── users/                    # Reserved for future user profile storage

logs/
├── server.log    # [SERVER] yyyy-MM-dd HH:mm:ss | event
├── chat.log      # [CHAT]   yyyy-MM-dd HH:mm:ss | message
└── errors.log    # [ERROR]  yyyy-MM-dd HH:mm:ss | exception
```

**File Format Rationale:**
- Plain text → universally readable, no serialization overhead
- Append-only → safe under concurrent writes (with `synchronized`)
- `tail()` helper → load only the last N lines without reading whole file

---

## Exception Handling Strategy

| Scenario | Handling |
|---|---|
| Invalid username (empty, too long, bad chars) | Loop with counter; disconnect after 3 failures |
| Duplicate username | Rejected with message; loop continues |
| Client disconnects abruptly | `IOException` caught in `run()`; `teardown()` called |
| Unknown command | Error message sent to client; loop continues |
| Non-existent room | Descriptive error; loop continues |
| User not in room | Descriptive error; loop continues |
| File write failure | Logged to errors.log; chat continues |
| File read failure | Return empty list; graceful no-history response |
| Port already in use | Fatal; `IOException` message + log; JVM exits |
| Invalid port argument | Warning printed; fallback to default 6000 |

**Principle:** the server never crashes because of user input.
Only unrecoverable resource errors (port binding failure) terminate the JVM.

---

## Algorithms Used

### 1 — Message Broadcasting
```
for each ClientHandler h in handlers.values():
    h.send(formattedMessage)
```
O(n) where n = connected clients.

### 2 — Room Delivery
```
for each member m in room.getMembers():
    handlers.get(m).send(formattedMessage)
```
O(k) where k = room members ≤ n.

### 3 — History Tail Read
```
read all lines into List<String>
if lines.size() > limit:
    return subList(size - limit, size)
```
O(L) where L = total lines in file (unavoidable for plain-text tail).

### 4 — Private Message Filename Canonical Form
```
sort([sender, recipient])    // alphabetical
filename = "pm_" + sorted[0] + "_" + sorted[1] + ".txt"
```
Ensures Alice→Bob and Bob→Alice share one file.

---

## Testing Strategy

### Manual Testing Checklist

| Test | Expected Result |
|---|---|
| Two clients connect with same username | Second client rejected with error |
| Broadcast message | All clients receive it |
| Private message to offline user | Sender gets "not online" error |
| `/create room1` + `/join room1` | User is in room, sees history |
| `@room1 Hello` without joining | Error: "not a member" |
| `/history` | Last 20 general messages displayed |
| Client kills terminal (CTRL-C) | Server logs disconnect; others notified |
| `/quit` | Graceful disconnect; others notified |
| Three+ concurrent clients | No message loss, no deadlock |
| Empty message (Enter key) | Silently ignored |
| `/unknownCmd` | "Unknown command" response |

### Suggested JUnit 5 Tests (Future)
- `AuthenticationService`: duplicate registration, removal, online count
- `RoomService`: create/join/leave, non-existent room, already-member
- `HistoryService`: save and load cycle, tail limit, private filename symmetry
- `ChatService`: message counter increment, correct type assignment
- `DateUtil`: format consistency

---

## Future Scope

1. **TLS/SSL** – Wrap sockets with `SSLSocket` for encrypted transport
2. **Password Auth** – BCrypt hashing stored in `data/users/`
3. **JavaFX GUI** – Rich client with tabbed rooms and notification badges
4. **REST Gateway** – Expose server state via HTTP for monitoring dashboards
5. **Docker** – Containerise server with a published port
6. **Horizontal Scaling** – Replace in-process `handlers` map with Redis pub/sub
7. **File Transfer** – Binary stream alongside text stream per connection
8. **JUnit 5 Suite** – Achieve ≥ 80 % line coverage on service layer

---

## Conclusion

This project demonstrates end-to-end software engineering discipline:
clean architecture, concurrent network programming, persistent storage, and
structured observability — all using only the Java standard library.

It is intentionally built without Spring, Netty, or any third-party dependency
to prove that first-principles knowledge translates directly into working systems.
The skills exercised here underpin every production backend, microservice, and
distributed system encountered in the industry.

---

*Real-Time Multi-Client Chat Application · Java 17 · Pure Sockets*
