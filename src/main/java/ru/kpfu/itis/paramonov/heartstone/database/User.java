package ru.kpfu.itis.paramonov.heartstone.database;

import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import java.util.List;

public class User {
    private int id;
    private String login;
    private String deck;

    private String cards;

    public User(int id, String login, String deck, String cards) {
        this.id = id;
        this.login = login;
        this.deck = deck;
        this.cards = cards;
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
}
