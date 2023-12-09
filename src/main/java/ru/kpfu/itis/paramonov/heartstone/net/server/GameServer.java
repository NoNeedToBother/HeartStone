package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONException;
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
        try {
            client.getOutput().write(message);
            client.getOutput().newLine();
            client.getOutput().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                    JSONObject json = new JSONObject(message);
                    String response;
                    if (checkEntityIsServer(json)) {
                        response = handleServerMessage(json);
                    }
                    else {
                        //stub
                        response = "response";
                    }

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

        private boolean checkEntityIsServer(JSONObject jsonMessage) {
            return (ServerMessage.Entity.SERVER.toString().equals(jsonMessage.getString("entity")));
        }

        private String handleServerMessage(JSONObject jsonServerMessage) {
            JSONObject response = new JSONObject();
            try {
                if (jsonServerMessage.getString("server_action").equals(ServerMessage.ServerAction.CONNECT.toString())) {
                    handleConnection(jsonServerMessage, response);
                }
            } catch (JSONException ignored) {
            }
            return response.toString();
        }

        private void handleConnection(JSONObject jsonServerMessage, JSONObject response) {
            switch (ServerMessage.ServerAction.valueOf(jsonServerMessage.getString("server_action"))) {
                case CONNECT -> {
                    response.put("server_action", "CONNECT");
                    Thread connectionThread = new Thread(getConnectionLogic());
                    connectionThread.start();
                    try {
                        connectionThread.join();
                        response.put("status", "OK");
                    } catch (InterruptedException e) {}
                }
            }
        }

        public BufferedWriter getOutput() {
            return output;
        }

        public BufferedReader getInput() {
            return input;
        }

        public GameServer getServer() {
            return server;
        }
    }
}
