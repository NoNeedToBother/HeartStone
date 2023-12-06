package ru.kpfu.itis.paramonov.heartstone.net.client;

import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.io.*;
import java.net.InetAddress;
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
            thread.getOutput().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        GameServer server = GameApplication.getApplication().getServer();
        InetAddress host = server.getHost();
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
        new Thread(thread).start();
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
                    String serverResponse = input.readLine();
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
