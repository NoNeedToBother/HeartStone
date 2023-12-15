package ru.kpfu.itis.paramonov.heartstone.model.card.card_info;

import ru.kpfu.itis.paramonov.heartstone.model.card.Card;

import java.util.ArrayList;
import java.util.List;

public class CardRepository {

    public enum CardAction {
        SUMMON, HP_UP, ATK_UP, HP_DOWN, ATK_DOWN, COST_UP, COST_DOWN, ATTACK, CHOOSE_RANDOM, CONSUME, FREEZE,
        CONTROL;

        private int hpIncrease;
        private int atkIncrease;
        private int costIncrease;

        private int damage;

        private List<Integer> targets;

        public CardAction setStats(int stats) {
            switch (this) {
                case ATK_UP -> atkIncrease = stats;
                case ATK_DOWN -> atkIncrease = -stats;
                case HP_UP-> hpIncrease = stats;
                case HP_DOWN -> hpIncrease = -stats;
                case COST_UP -> costIncrease = stats;
                case COST_DOWN -> costIncrease = -stats;
                case ATTACK -> damage = stats;
                default -> throw new RuntimeException("Card does not support stats increase");
            }
            return this;
        }

        public CardAction setTargets(int... targets) {
            this.targets = new ArrayList<Integer>();
            for (int target : targets) {
                this.targets.add(target);
            }
            return this;
        }
    }

    public enum KeyWord {
        BATTLE_CRY("Battle cry", "Triggers when card is played from hand"),
        LAST_WISH("Last wish", "Triggers when card hp is less than "),
        CONSUME("Consume", "Destroys other card to trigger effect"),
        REVIVE("Revive", "Summons a card that was once on battlefield"),
        PUNISHMENT("Punishment", "Triggers when card is attacked and survives damage"),
        BURN("Burn", "At the end of turn burned opponent card suffers damage");

        private String displayName;

        private String description;

        KeyWord(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum Faction {
        NO_FACTION, STONE, ELEMENTAL //etc
    }

    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY
    }

    private static final String DEFAULT_PATH = "/assets/images/cards";

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

    public static List<CardTemplate> getCardsById(List<Integer> ids) {
        List<CardTemplate> res = new ArrayList<>();
        for (Integer id : ids) {
            res.add(getCardTemplate(id));
        }
        return res;
    }

    public static List<Integer> getCardsByRarity(Rarity rarity) {
        List<Integer> res = new ArrayList<>();
        for (CardTemplate cardTemplate : CardTemplate.values()) {
            if (cardTemplate.getRarity().equals(rarity)) res.add(cardTemplate.id);
        }
        return res;
    }

    public enum CardTemplate {
        Stone(1, "Stone", 1, 1, 0, "", DEFAULT_PATH + "/basic_stone.png",
                List.of(), List.of(), Faction.STONE, Rarity.COMMON),

        KnightStone(2, "Stoneland knight", 4, 5, 6, "Battlecry: gives +2/2 to random friendly stone", DEFAULT_PATH + "/knight_stone.png",
                List.of(CardAction.CHOOSE_RANDOM, CardAction.ATK_UP.setStats(2), CardAction.HP_UP.setStats(2)), List.of(KeyWord.BATTLE_CRY),
                Faction.STONE, Rarity.RARE),

        FireElemental(3, "Fire elemental", 1, 1, 2, "Battlecry: deal 2 damage to random enemy",
                DEFAULT_PATH + "/fire_elemental.png", List.of(CardAction.CHOOSE_RANDOM, CardAction.ATTACK.setStats(2)), List.of(KeyWord.BATTLE_CRY),
                Faction.ELEMENTAL, Rarity.COMMON),

        FirePack(4, "Fire elemental pack", 2, 2, 5, "Consume: fire elemental to get +2/2",
                DEFAULT_PATH + "/fire_pack.png", List.of(CardAction.CONSUME.setTargets(3)), List.of(KeyWord.CONSUME),
                Faction.ELEMENTAL, Rarity.EPIC),

        IceElemental(5, "Ice elemental", 2, 1, 1, "Battlecry: freezes random enemy", DEFAULT_PATH + "/ice_elemental.png",
                List.of(CardAction.CHOOSE_RANDOM, CardAction.FREEZE), List.of(KeyWord.BATTLE_CRY), Faction.ELEMENTAL, Rarity.COMMON),

        TheRock(6, "The Rock", 4, 4, 5, "Punishment: attacks enemy hero and gains +1/1", DEFAULT_PATH + "/dwayne_rock.png",
                List.of(CardAction.ATTACK, CardAction.HP_UP.setStats(1), CardAction.ATK_UP.setStats(1)), List.of(KeyWord.PUNISHMENT), Faction.NO_FACTION, Rarity.LEGENDARY),

        HypnoShroom(7, "Hypnoshroom", 4, 6, 6, "When turn ends, gains control of random opponent card",
                DEFAULT_PATH + "/hypnoshroom.png", List.of(CardAction.CHOOSE_RANDOM, CardAction.CONTROL),
                List.of(), Faction.NO_FACTION, Rarity.LEGENDARY),

        Whelp(8, "Dragon whelp", 2, 1, 4, "Battlecry: burn random enemy (2)", DEFAULT_PATH + "/whelp.png",
                List.of(CardAction.CHOOSE_RANDOM, CardAction.ATTACK), List.of(KeyWord.BATTLE_CRY, KeyWord.BURN), Faction.NO_FACTION, Rarity.RARE),

        Phoenix(9, "Phoenix", 2, 2, 4, "Last wish: reborns and gains +2/1", DEFAULT_PATH + "/phoenix.png",
                List.of(CardAction.ATK_UP.setStats(2), CardAction.HP_UP.setStats(1)), List.of(KeyWord.LAST_WISH),
                Faction.ELEMENTAL, Rarity.EPIC),

        StoneGiant(10, "Stone giant", 6, 6, 10, "Cost -1 for each your destroyed stone card", DEFAULT_PATH + "/stone_giant.png",
                List.of(CardAction.COST_DOWN.setStats(1)), List.of(), Faction.STONE, Rarity.EPIC);


        private int id;

        private String name;

        private int hp;

        private int atk;

        private int cost;

        private String actionDesc;

        private String portraitUrl;

        private List<CardAction> actions;

        private List<KeyWord> keyWords;

        private Faction faction;

        private Rarity rarity;

        CardTemplate(int id, String name, int hp, int atk, int cost, String actionDesc, String imageUrl, List<CardAction> actions,
                     List<KeyWord> keyWords, Faction faction, Rarity rarity) {
            this.id = id;
            this.name = name;
            this.hp = hp;
            this.atk = atk;
            this.cost = cost;
            this.actionDesc = actionDesc;
            this.portraitUrl = imageUrl;
            this.actions = actions;
            this.keyWords = keyWords;
            this.faction = faction;
            this.rarity = rarity;
        }

        public String getName() {
            return name;
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

        public String getActionDesc() {
            return actionDesc;
        }
    }
}
