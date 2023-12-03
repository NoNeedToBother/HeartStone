package ru.kpfu.itis.paramonov.heartstone.model.card.card_info;

import org.controlsfx.control.action.Action;

import java.util.List;

public class CardRepository {

    public enum CardAction {
        SUMMON, HP_UP, ATK_UP, HP_DOWN, ATK_DOWN, REVIVE, ATTACK, //etc
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


    public enum CardTemplate {
        Stone(1, 1, 1, 0, "Just an ordinary stone", "/basic_stone.png", null, null, Faction.STONE, Rarity.COMMON);

        private int id;

        private int hp;

        private int atk;

        private int cost;

        private String description;

        private String imageUrl;

        private List<Action> actions;

        private List<KeyWord> keyWords;

        private Faction faction;

        private Rarity rarity;

        CardTemplate(int id, int hp, int atk, int cost, String description, String imageUrl, List<Action> actions,
                     List<KeyWord> keyWords, Faction faction, Rarity rarity) {
            this.id = id;
            this.hp = hp;
            this.atk = atk;
            this.cost = cost;
            this.description = description;
            this.imageUrl = imageUrl;
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

        public String getImageUrl() {
            return imageUrl;
        }

        public List<Action> getActions() {
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
