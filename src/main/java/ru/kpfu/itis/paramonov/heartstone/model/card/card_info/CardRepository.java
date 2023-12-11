package ru.kpfu.itis.paramonov.heartstone.model.card.card_info;


import ru.kpfu.itis.paramonov.heartstone.model.card.Card;

import java.util.ArrayList;
import java.util.List;

public class CardRepository {

    public enum CardAction {
        SUMMON, HP_UP, ATK_UP, HP_DOWN, ATK_DOWN, COST_UP, COST_DOWN, REVIVE, ATTACK, CHOOSE, CHOOSE_RANDOM;

        private int hpIncrease;
        private int atkIncrease;
        private int costIncrease;

        public CardAction setStats(int stats) {
            switch (this) {
                case ATK_UP -> atkIncrease = stats;
                case ATK_DOWN -> atkIncrease = -stats;
                case HP_UP-> hpIncrease = stats;
                case HP_DOWN -> hpIncrease = -stats;
                case COST_UP -> costIncrease = stats;
                case COST_DOWN -> costIncrease = -stats;
                default -> throw new RuntimeException("Card does not support stats increase");
            }
            return this;
        }
    }

    public enum KeyWord {
        BATTLE_CRY, LAST_WISH, //etc
    }

    public enum Faction {
        STONE, //etc
    }

    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY
    }

    private static final String DEFAULT_PATH = "D:/projects/HeartStone/src/main/resources/assets/images";

    public static CardTemplate getCardTemplate(int id) {
        for (CardTemplate card : CardTemplate.values()) {
            if (card.getId() == id) return card;
        }
        throw new RuntimeException("Card does not exist");
    }

    public static List<CardTemplate> getCardsById(String cardIds) {
        String cardIdsCsv = cardIds.substring(1, cardIds.length() - 1);
        String[] ids = cardIdsCsv.split(",");
        List<CardTemplate> res = new ArrayList<>();
        for (String id : ids) {
            res.add(getCardTemplate(Integer.parseInt(id)));
        }
        return res;
    }

    public enum CardTemplate {
        Stone(1, 1, 1, 0, "", "Just an ordinary stone", DEFAULT_PATH + "/basic_stone.png",
                null, null, Faction.STONE, Rarity.COMMON),
        KnightStone(2, 4, 5, 6, "Battlecry: gives +2/2 to chosen stone", "On guard of Stoneland since childhood", DEFAULT_PATH + "/knight_stone.png",
                List.of(CardAction.CHOOSE, CardAction.ATK_UP.setStats(2), CardAction.HP_UP.setStats(2)), List.of(KeyWord.BATTLE_CRY),
                Faction.STONE, Rarity.RARE);

        private int id;

        private int hp;

        private int atk;

        private int cost;

        private String actionDesc;

        private String description;

        private String portraitUrl;

        private List<CardAction> actions;

        private List<KeyWord> keyWords;

        private Faction faction;

        private Rarity rarity;

        CardTemplate(int id, int hp, int atk, int cost, String actionDesc, String description, String imageUrl, List<CardAction> actions,
                     List<KeyWord> keyWords, Faction faction, Rarity rarity) {
            this.id = id;
            this.hp = hp;
            this.atk = atk;
            this.cost = cost;
            this.actionDesc = actionDesc;
            this.description = description;
            this.portraitUrl = imageUrl;
            this.actions = actions;
            this.keyWords = keyWords;
            this.faction = faction;
            this.rarity = rarity;
        }

        public int getHp() {
            return hp;
        }

        public int getAtk() {
            return atk;
        }

        public int getCost() {
            return cost;
        }

        public String getDescription() {
            return description;
        }

        public String getPortraitUrl() {
            return portraitUrl;
        }

        public List<CardAction> getActions() {
            return actions;
        }

        public List<KeyWord> getKeyWords() {
            return keyWords;
        }

        public Faction getFaction() {
            return faction;
        }

        public int getId() {
            return id;
        }

        public Rarity getRarity() {
            return rarity;
        }
    }
}
