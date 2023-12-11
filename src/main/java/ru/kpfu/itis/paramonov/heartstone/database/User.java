package ru.kpfu.itis.paramonov.heartstone.database;

public class User {
    private int id;
    private String login;
    private String deck;

    private String cards;

    private int money;

    public User(int id, String login, String deck, String cards, int money) {
        this.id = id;
        this.login = login;
        this.deck = deck;
        this.cards = cards;
        this.money = money;
    }

    public int getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getDeck() {
        return deck;
    }

    public String getCards() {
        return cards;
    }

    public int getMoney() {
        return money;
    }
}
