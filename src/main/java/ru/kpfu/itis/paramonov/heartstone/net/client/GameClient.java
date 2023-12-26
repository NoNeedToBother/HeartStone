package ru.kpfu.itis.paramonov.heartstone.net.client;

import javafx.application.Platform;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;
import ru.kpfu.itis.paramonov.heartstone.util.ClientRoomMsgHandler;
import ru.kpfu.itis.paramonov.heartstone.util.ClientServerMsgHandler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class GameClient {
    private GameApplication application;

    private Socket socket;

    private ClientThread thread;

    public GameClient(GameApplication application) {
        this.application = application;
    }

    public void sendMessage(String message) {
        try {
            thread.getOutput().write(message);
            thread.getOutput().newLine();
            thread.getOutput().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        GameServer server = application.getServer();
        String host = server.getHost();
        int port = server.getPort();

        BufferedReader input;
        BufferedWriter output;
        try {
            socket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        thread = new ClientThread(input, output, this);
        (new Thread(thread)).start();
    }

    public GameApplication getApplication() {
        return application;
    }

    static class ClientThread implements Runnable {

        private BufferedReader input;
        private BufferedWriter output;
        private GameClient gameClient;

        public ClientThread(BufferedReader input, BufferedWriter output, GameClient gameClient) {
            this.input = input;
            this.output = output;
            this.gameClient = gameClient;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String response = input.readLine();
                    System.out.println(response);
                    Platform.runLater(() -> {
                        ClientServerMsgHandler serverMsgHandler = new ClientServerMsgHandler();
                        serverMsgHandler.handle(response);
                        ClientRoomMsgHandler roomMsgHandler = new ClientRoomMsgHandler();
                        roomMsgHandler.handle(response);
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public BufferedWriter getOutput() {
            return output;
        }
    }
}
