package ru.kpfu.itis.paramonov.heartstone.database;

import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import java.util.List;

public class User {
    private int id;
    private String login;
    private List<CardRepository.CardTemplate> deck;

    private List<CardRepository.CardTemplate> cards;

    public User(int id, String login, List<CardRepository.CardTemplate> deck, List<CardRepository.CardTemplate> cards) {
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

    public List<CardRepository.CardTemplate> getDeck() {
        return deck;
    }

    public List<CardRepository.CardTemplate> getCards() {
        return cards;
    }
}
