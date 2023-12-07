package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GameServer {
    private ServerSocket serverSocket;
    private List<Client> clients = new ArrayList<>();

    private List<Client> clientsToConnect = new ArrayList<>();

    private final String host = "127.0.0.1";

    private final int port = 5555;

    private GameServer() {}

    private static GameServer gameServer = null;

    public static GameServer getInstance() {
        if (gameServer == null) {
            gameServer = new GameServer();
        }
        return gameServer;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));

                Client client = new Client(input, output, this);
                clients.add(client);

                (new Thread(client)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendResponse(String message, Client client) {
        for (Client c : clients) {
            if (c.equals(client)) {
                try {
                    c.getOutput().write(message);
                    c.getOutput().newLine();
                    c.getOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void main(String[] args) {
        GameServer gameServer = new GameServer();
        gameServer.start();
    }

    static class Client implements Runnable {

        private BufferedReader input;
        private BufferedWriter output;
        private GameServer server;

        private boolean connected = false;

        public Client(BufferedReader input, BufferedWriter output, GameServer gameServer) {
            this.input = input;
            this.output = output;
            this.server = gameServer;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = input.readLine();
                    String response = handleMessage(message);

                    server.sendResponse(response, this);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void notifyConnected() {
            connected = true;
        }

        private Runnable getConnectionLogic() {
            Runnable runnable = () -> {
                try {
                    server.clientsToConnect.add(this);
                    while (!connected) {
                        Thread.sleep(50);
                        for (Client otherClient : server.clientsToConnect) {
                            if (!this.equals(otherClient)) {
                                otherClient.notifyConnected();
                                GameRoom room = new GameRoom(this, otherClient);
                                connected = true;
                                server.clientsToConnect.remove(this);
                                server.clientsToConnect.remove(otherClient);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
            return runnable;
        }

        private String handleMessage(String serverMessage) {
            JSONObject json = new JSONObject(serverMessage);
            JSONObject response = new JSONObject();
            response.put("action", "CONNECT");
            if (json.getString("entity").equals(ServerMessage.Entity.SERVER.toString())) {
                switch (ServerMessage.ServerAction.valueOf(json.getString("server_action"))) {
                    case CONNECT -> {
                        Thread connectionThread = new Thread(getConnectionLogic());
                        connectionThread.start();
                        try {
                            connectionThread.join();
                            response.put("status", "OK");
                        } catch (InterruptedException e) {}
                    }
                }

            } else {
                response.put("status", "NOT_OK");
                response.put("reason", "INCORRECT_ENTITY");
            }
            return response.toString();
        }

        public BufferedWriter getOutput() {
            return output;
        }
    }
}
