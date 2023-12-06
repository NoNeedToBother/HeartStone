package ru.kpfu.itis.paramonov.heartstone.net;

public class ServerMessage {

    private enum Action {
        CONNECT, //etc
    }

    private static ServerMessageBuilder builder = new ServerMessageBuilder();

    private static class ServerMessageBuilder {
        ServerMessage message = new ServerMessage();

        public ServerMessage build() {
            return message;
        }
    }

    public static ServerMessageBuilder builder() {
        return builder;
    }
}
