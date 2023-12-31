package ru.kpfu.itis.paramonov.heartstone.database;

public record User(int id, String login, String deck, String cards, int money) {
}
