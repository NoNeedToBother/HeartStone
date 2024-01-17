package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerData {
    private Hero hero;

    private Map<String, Integer> battleValues = setBattleValues();

    private Map<String, List<Card>> allCards = setPlayerCardMap();

    private Map<String, List<Card>> setPlayerCardMap() {
        Map<String, List<Card>> res = new HashMap<>();
        res.put("field", new ArrayList<>());
        res.put("hand", new ArrayList<>());
        res.put("deck", new ArrayList<>());
        res.put("played", new ArrayList<>());
        res.put("defeated", new ArrayList<>());
        return res;
    }

    private Map<String, Integer> setBattleValues() {
        Map<String, Integer> res = new HashMap<>();
        res.put("burntCardDmg", 0);
        return res;
    }

    public Map<String, List<Card>> getAllCards() {
        return allCards;
    }

    public List<Card> getField() {
        return allCards.get("field");
    }

    public List<Card> getHand() {
        return allCards.get("hand");
    }

    public void setHand(List<Card> hand) {
        allCards.put("hand", hand);
    }

    public List<Card> getDeck() {
        return allCards.get("deck");
    }

    public void setDeck(List<Card> deck) {
        allCards.put("deck", deck);
    }

    public List<Card> getPlayedCards() {
        return allCards.get("played");
    }

    public List<Card> getDefeatedCards() {
        return allCards.get("defeated");
    }

    public Hero getHero() {
        return hero;
    }

    public void setHero(Hero hero) {
        this.hero = hero;
    }

    public int getBurntCardDamage() {
        return battleValues.get("burntCardDmg");
    }

    public void incrementBurntCardDamage() {
        Integer dmg = battleValues.get("burntCardDmg");
        dmg++;
        battleValues.put("burntCardDmg", dmg);
    }

    public void clear() {
        allCards = null;
        battleValues = null;
        hero = null;
    }
}
