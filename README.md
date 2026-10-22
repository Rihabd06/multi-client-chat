# Multi-Client Chat Application (Java Sockets + JavaFX)

Real-time group chat over **TCP**. A central **TCPServer** receives each message and distributes it to every connected **TCPClient**. Built with Java 21 (virtual threads), JavaFX, Maven.

## Features

**Client:** username login · READ-ONLY MODE when no username is given · send with `SEND` button or Enter · `allUsers` command lists active users (to the requester only) · disconnect with `LEAVE`, `end` or `bye` · "Online" label + coloured status circle.

**Server:** many concurrent clients (one **virtual thread** per client) · messages formatted with sender + time (`[HH:mm:ss] user: text`) and broadcast to all · live `ListView` of connected users with random pastel background per user · activity log (`Server Started`, `Waiting for Client...`, `Welcome <user>`, ...) · duplicate usernames rejected.

## Architecture

Strict **Model / View separation**. The `model` packages contain all socket and business logic and **do not import JavaFX**; the `view` packages only render and forward user input. They communicate through listener interfaces (`ServerListener`, `ChatClientListener`).

```
TCPServer/  src/main/java
  TCPServer.java                  entry point  (java TCPServer)
  chat/server/model/              ChatServer, ClientHandler, ServerConfig, ServerListener   (no JavaFX)
  chat/server/view/ServerApp.java JavaFX GridPane UI
  src/main/resources              config.properties, style.css
TCPClient/  src/main/java
  TCPClient.java                  entry point  (java TCPClient <ip> <port>)
  chat/client/model/              ChatClient, ClientConfig, ChatMessage, ChatClientListener (no JavaFX)
  chat/client/view/ClientApp.java JavaFX GridPane UI
  src/main/resources              config.properties, style.css
docs/                             UML diagrams (class, deployment, sequence, use case)
```

### Protocol (one UTF-8 line per message, `|` separated)
| Direction | Lines |
|---|---|
| client → server | `JOIN|username` (empty = read-only) · `MSG|text` · `USERS` · `BYE` |
| server → client | `INFO|text` · `CHAT|time|user|text` · `USERS|a,b,c` · `ERROR|text` |

## Configuration (no recompilation needed)

`config.properties` holds the server IP and port. A copy placed **next to the JAR** overrides the bundled default.

```properties
server.ip=0.0.0.0     # server bind address (client: server to connect to)
server.port=3000
```
The client's command-line arguments override its properties file.

## Requirements
JDK 21+, Maven 3.9+ (JavaFX is downloaded by Maven).

## Build
```bash
cd TCPServer && mvn clean package      # -> target/TCPServer.jar
cd ../TCPClient && mvn clean package   # -> target/TCPClient.jar
```
> The JAR bundles the JavaFX natives of the OS it was built on. Build on the OS you will run on.

## Run
```bash
java -jar TCPServer/target/TCPServer.jar                       # server (equivalent of: java TCPServer)
java -jar TCPClient/target/TCPClient.jar localhost 3000        # client (equivalent of: java TCPClient localhost 3000)
```
or from source: `mvn javafx:run` inside each project.

## Usage
1. Start the server — it logs `Server Started` and `Waiting for Client...`.
2. Start one or more clients, enter a username, click **CONNECT**.
3. Leave the username empty to join in **READ-ONLY MODE**.
4. Type `allUsers` to list active users; `end`, `bye` or **LEAVE** to disconnect.

## UML
See [`docs/`](docs): `class_diagram.png`, `deployment_diagram.png`, plus sequence and use-case diagrams in `diagrams.puml` (PlantUML).
