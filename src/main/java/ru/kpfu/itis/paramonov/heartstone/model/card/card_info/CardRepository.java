package ru.kpfu.itis.paramonov.heartstone.model.card.card_info;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CardRepository {

    public enum CardAction {
        RUSH_ON_PLAY, DAMAGE_ENEMY_ON_PLAY, DESTROY_ENEMY_ON_PLAY, DAMAGE_ON_PLAY, HP_UP, ATK_UP, FREEZE_ENEMY_ON_PLAY,
        ON_END_TURN, COST_DOWN, IGNORE_TAUNT, DAMAGE_HERO_ON_DMG, DRAW_CARD_ON_PLAY
    }

    public enum KeyWord {
        BATTLE_CRY("Battle cry", "Triggers when card is played from hand"),
        TAUNT("Taunt", "Cards must attack card with this status"),
        PUNISHMENT("Punishment", "Triggers when card is attacked and survives damage"),
        FREEZE("Freeze", "Cannot attack for 1 turn"),
        RUSH("Rush", "Can  attack immediately after played"),
        DESTROY("Destroy", "Sets chosen enemy's hp to zero"),
        GIANT("Giant", "Decreases cost when conditions are met");

        private final String displayName;

        private final String description;

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
        NO_FACTION, STONE, ELEMENTAL, ANIMAL, ROBOT //etc
    }

    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY
    }

    public enum Status {
        FROZEN(false, "frozen"), CANNOT_ATTACK(false, "cannot attack"), ATTACKED(true, null),
        FROZEN_1(true, null), FROZEN_2(true, null);

        private final boolean utility;
        private final String displayName;

        Status(boolean utility, String displayName) {
            this.utility = utility;
            this.displayName = displayName;
        }

        public boolean isUtility() {
            return utility;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static List<Status> getCardAttackRestrictionStatuses() {
            return List.of(FROZEN, FROZEN_1, FROZEN_2, CANNOT_ATTACK, ATTACKED);
        }
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
                List.of(), List.of(), Faction.STONE, Rarity.COMMON, Map.of()),

        KnightStone(2, "Stoneland knight", 4, 4, 4, "Taunt", DEFAULT_PATH + "/knight_stone.png",
                List.of(), List.of(KeyWord.TAUNT), Faction.STONE, Rarity.RARE, Map.of()),
        FireElemental(3, "Fire elemental", 1, 1, 2, "Battlecry: deal 2 damage to chosen enemy",
                DEFAULT_PATH + "/fire_elemental.png", List.of(CardAction.DAMAGE_ENEMY_ON_PLAY), List.of(KeyWord.BATTLE_CRY),
                Faction.ELEMENTAL, Rarity.COMMON, Map.of(StatChange.DMG, 2)),
        FirePack(4, "Fire elemental pack", 5, 5, 5, "",
                DEFAULT_PATH + "/fire_pack.png", List.of(), List.of(), Faction.ELEMENTAL, Rarity.COMMON, Map.of()),
        IceElemental(5, "Ice elemental", 2, 1, 1, "Battlecry: freezes chosen enemy", DEFAULT_PATH + "/ice_elemental.png",
                List.of(CardAction.FREEZE_ENEMY_ON_PLAY), List.of(KeyWord.BATTLE_CRY, KeyWord.FREEZE), Faction.ELEMENTAL, Rarity.COMMON, Map.of()),
        TheRock(6, "The Rock", 4, 4, 5, "Punishment: deals damage equal to its attack to enemy hero and then gains +2/1", DEFAULT_PATH + "/dwayne_rock.png",
                List.of(CardAction.DAMAGE_HERO_ON_DMG), List.of(KeyWord.PUNISHMENT), Faction.NO_FACTION,
                Rarity.LEGENDARY, Map.of(StatChange.ATK, 2, StatChange.HP, 1)),
        HypnoShroom(7, "Hypnoshroom", 6, 4, 6, "When turn ends, gains control of random opponent card",
                DEFAULT_PATH + "/hypnoshroom.png", List.of(CardAction.ON_END_TURN), List.of(), Faction.NO_FACTION, Rarity.LEGENDARY, Map.of()),
        Whelp(8, "Dragon whelp", 3, 5, 4, "", DEFAULT_PATH + "/whelp.png",
                List.of(), List.of(), Faction.ANIMAL, Rarity.COMMON, Map.of()),
        Phoenix(9, "Phoenix", 4, 4, 4, "Battlecry: deal 2 damage to chosen enemy", DEFAULT_PATH + "/phoenix.png",
                List.of(), List.of(KeyWord.BATTLE_CRY), Faction.ELEMENTAL, Rarity.RARE, Map.of(StatChange.DMG, 2)),
        StoneGiant(10, "Stone giant", 6, 6, 10, "Giant: cost -1 for each played stone", DEFAULT_PATH + "/stone_giant.png",
                List.of(), List.of(KeyWord.GIANT), Faction.STONE, Rarity.EPIC, Map.of(StatChange.COST, -1)),
        FierceTiger(11, "Fierce tiger", 3, 3, 4, "Charge", DEFAULT_PATH + "/tiger.png", List.of(CardAction.RUSH_ON_PLAY), List.of(KeyWord.RUSH), Faction.ANIMAL,
                Rarity.RARE, Map.of()),
        StoneAssassin(12, "Stone assassin", 2, 2, 5, "Battlecry: destroy chosen enemy", DEFAULT_PATH + "/stone_assassin.png",
                List.of(CardAction.DESTROY_ENEMY_ON_PLAY), List.of(KeyWord.BATTLE_CRY, KeyWord.DESTROY), Faction.STONE, Rarity.EPIC, Map.of()),
        CrazyPyromaniac(13, "Crazy pyromaniac", 4, 4, 5, "Battlecry: deals 2 damage to all other cards", DEFAULT_PATH + "/crazy_pyromaniac.png",
                List.of(CardAction.DAMAGE_ON_PLAY), List.of(KeyWord.BATTLE_CRY), Faction.ELEMENTAL, Rarity.RARE, Map.of(StatChange.ALL_DMG, 2)),
        Trantos(14, "Tran'tos", 3, 3, 6, "Battlecry: deals 3 damage to all other cards. For each defeated card +2/1",
                DEFAULT_PATH + "/trantos.png", List.of(CardAction.DAMAGE_ON_PLAY, CardAction.HP_UP, CardAction.ATK_UP),
                List.of(KeyWord.BATTLE_CRY), Faction.ELEMENTAL, Rarity.LEGENDARY,
                Map.of(StatChange.ALL_DMG, 3, StatChange.ATK, 2, StatChange.HP, 1)),
        TheGreatestApex(15, "The greatest apex", 7, 7, 8, "Charge", DEFAULT_PATH + "/greatest_apex.png",
                List.of(CardAction.RUSH_ON_PLAY), List.of(KeyWord.RUSH), Faction.ANIMAL, Rarity.LEGENDARY, Map.of()),
        Slime(16, "Slime", 2, 2, 1, "", DEFAULT_PATH + "/slime.png", List.of(), List.of(),
                Faction.NO_FACTION, Rarity.COMMON, Map.of()),
        WiseTree(17, "Wise mysterious tree", 2, 3, 2, "Taunt", DEFAULT_PATH + "/wise_tree.png",
                List.of(), List.of(KeyWord.TAUNT), Faction.NO_FACTION, Rarity.COMMON, Map.of()),
        WaterGiant(18, "Water giant", 5, 4, 10, "Giant: cost -1 for each played elemental. Rush", DEFAULT_PATH + "/water_giant.png",
                List.of(CardAction.RUSH_ON_PLAY), List.of(KeyWord.GIANT, KeyWord.RUSH), Faction.ELEMENTAL, Rarity.EPIC, Map.of(StatChange.COST, -1)),
        STAN_3000(19, "STAN-3000", 7, 7, 12, "Giant: cost -1 for each played robot. Taunt", DEFAULT_PATH + "/stan3000.png",
                List.of(), List.of(KeyWord.GIANT, KeyWord.TAUNT), Faction.ROBOT, Rarity.EPIC, Map.of(StatChange.COST, -1)),
        A_50(20, "A-50", 3, 3, 3, "", DEFAULT_PATH + "/a50.png", List.of(), List.of(),
                Faction.ROBOT, Rarity.COMMON, Map.of()),
        B_30(21, "B-30", 3, 2, 6, "Battlecry: deals 5 damage. Rush",DEFAULT_PATH + "/b30.png", List.of(CardAction.DAMAGE_ENEMY_ON_PLAY, CardAction.RUSH_ON_PLAY),
                List.of(KeyWord.BATTLE_CRY, KeyWord.RUSH), Faction.ROBOT, Rarity.RARE, Map.of(StatChange.DMG, 5)),
        TENS_100(22, "TENS-1000", 4, 5, 5, "Rush. Ignores taunt", DEFAULT_PATH + "/tens100.png",
                List.of(CardAction.RUSH_ON_PLAY, CardAction.IGNORE_TAUNT), List.of(KeyWord.RUSH), Faction.ROBOT, Rarity.LEGENDARY, Map.of()),
        Ninja(23, "Ninja", 4, 4, 5, "Ignores taunt", DEFAULT_PATH + "/ninja.png", List.of(CardAction.IGNORE_TAUNT),
                List.of(), Faction.NO_FACTION, Rarity.RARE, Map.of()),
        IceSorcerer(24, "Ice sorcerer", 4,4, 5, "Battlecry: freezes chosen enemy", DEFAULT_PATH + "/ice_sorcerer.png",
                List.of(CardAction.FREEZE_ENEMY_ON_PLAY), List.of(KeyWord.BATTLE_CRY, KeyWord.FREEZE), Faction.ELEMENTAL, Rarity.RARE, Map.of()),
        Rat(25, "Rat", 3, 2, 2, "", DEFAULT_PATH + "/rat.png", List.of(), List.of(), Faction.ANIMAL, Rarity.COMMON, Map.of()),
        Dragon(26, "The Dragon", 8, 8, 8, "Battlecry: draw a card. Taunt", DEFAULT_PATH + "/dragon.png", List.of(CardAction.DRAW_CARD_ON_PLAY),
                List.of(KeyWord.BATTLE_CRY, KeyWord.TAUNT), Faction.ANIMAL, Rarity.RARE, Map.of(StatChange.CARD_DRAWN, 1)),
        Illusionist(27, "Illusionist", 3, 4, 3, "Battlecry: both player draw 2 cards", DEFAULT_PATH + "/illusionist.png", List.of(CardAction.DRAW_CARD_ON_PLAY),
                List.of(KeyWord.BATTLE_CRY), Faction.NO_FACTION, Rarity.EPIC, Map.of(StatChange.CARD_DRAWN, 2)),
        ProfessorDog(28, "Professor Dog", 3, 3, 5, "Whenever you draw a card gains +2/2", DEFAULT_PATH + "/prof_dog.png", List.of(),
                List.of(), Faction.ANIMAL, Rarity.LEGENDARY, Map.of(StatChange.ATK, 2, StatChange.HP, 2)),

        SlimeWarrior(29, "Slime warrior", 4, 5, 5, "Taunt", DEFAULT_PATH + "/warrior_slime.png",
                List.of(), List.of(KeyWord.TAUNT), Faction.NO_FACTION, Rarity.COMMON, Map.of()),
        SlimeCommander(30, "Slime commander", 5, 5, 7, "Punishment: deals 4 damage to opponent hero",
                DEFAULT_PATH + "/slime_commander.png", List.of(CardAction.DAMAGE_HERO_ON_DMG), List.of(KeyWord.BATTLE_CRY),
                Faction.NO_FACTION, Rarity.EPIC, Map.of(StatChange.HERO_DMG, 4)),
        Postman(31, "Postman", 3, 3, 4, "At the end of turn draws a card", DEFAULT_PATH + "/postman.png",
                List.of(), List.of(), Faction.NO_FACTION, Rarity.RARE, Map.of(StatChange.CARD_DRAWN, 1)),
        MerchantRobot(32, "Merchant robot", 2, 2, 3, "Battlecry: draw a card", DEFAULT_PATH + "/merchant_robot.png",
                List.of(CardAction.DRAW_CARD_ON_PLAY), List.of(KeyWord.BATTLE_CRY), Faction.ROBOT, Rarity.RARE, Map.of(StatChange.CARD_DRAWN, 1)),
        BraveRat(33, "Brave rat", 4, 2, 4, "Rush", DEFAULT_PATH + "/brave_rat.png",
                List.of(CardAction.RUSH_ON_PLAY), List.of(KeyWord.RUSH), Faction.ANIMAL, Rarity.COMMON, Map.of()),
        SwampThing(34, "Swamp thing", 4, 6, 5, "Taunt", DEFAULT_PATH + "/swamp_thing.png", List.of(),
                List.of(KeyWord.TAUNT), Faction.NO_FACTION, Rarity.COMMON, Map.of()),
        Hydra(35, "Hydra", 4, 5, 7, "Battlecry: destroys an enemy", DEFAULT_PATH + "/hydra.png", List.of(CardAction.DESTROY_ENEMY_ON_PLAY),
                List.of(KeyWord.BATTLE_CRY, KeyWord.DESTROY), Faction.ANIMAL, Rarity.EPIC, Map.of()),
        AnimalKing(36, "King of animals", 6, 6, 10, "Giant: cost -1 for each played animal", DEFAULT_PATH + "/animal_king.png",
                List.of(), List.of(KeyWord.GIANT), Faction.ANIMAL, Rarity.EPIC, Map.of(StatChange.COST, -1)),
        Deity(37, "His deity", 8, 8, 20, "Giant: cost -6 for each played giant", DEFAULT_PATH + "/his_deity.png",
                List.of(), List.of(KeyWord.GIANT), Faction.NO_FACTION, Rarity.LEGENDARY, Map.of(StatChange.COST, -6)),
        BuriedColossus(38, "Buried colossus", 4, 8, 5, "Taunt. Cannot attack", DEFAULT_PATH + "/buried_colossus.png",
                List.of(), List.of(KeyWord.TAUNT), Faction.STONE, Rarity.RARE, Map.of()),
        HeartStone(39, "HeartStone", 3, 3, 4, "Taunt. Cannot attack. Punishment: deals 1 damage to enemy hero",
                DEFAULT_PATH + "/heart_stone.png", List.of(), List.of(KeyWord.TAUNT, KeyWord.PUNISHMENT),
                Faction.STONE, Rarity.RARE, Map.of(StatChange.HERO_DMG, 1)),
        RushingHound(40, "Rushing hound", 2, 1, 2, "Rush", DEFAULT_PATH + "/hound.png", List.of(CardAction.RUSH_ON_PLAY),
                List.of(KeyWord.RUSH), Faction.ANIMAL, Rarity.COMMON, Map.of());

        private enum StatChange {
            ATK, HP, COST, DMG, ALL_DMG, HERO_DMG, CARD_DRAWN
        }

        private final int id;

        private final String name;

        private final int hp;

        private final int atk;

        private final int cost;

        private final String actionDesc;

        private final String portraitUrl;

        private final List<CardAction> actions;

        private final List<KeyWord> keyWords;

        private final Faction faction;

        private final Rarity rarity;

        private final Map<StatChange, Integer> statChanges;

        CardTemplate(int id, String name, int atk, int hp, int cost, String actionDesc, String imageUrl, List<CardAction> actions,
                     List<KeyWord> keyWords, Faction faction, Rarity rarity, Map<StatChange, Integer> statChanges) {
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
            this.statChanges = statChanges;
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

        public Integer getAtkIncrease() {
            return statChanges.get(StatChange.ATK);
        }

        public Integer getHpIncrease() {
            return statChanges.get(StatChange.HP);
        }

        public Integer getCostDecrease() {
            Integer res = statChanges.get(StatChange.COST);
            if (res != null) return -res;
            else return null;
        }

        public Integer getHeroDamage() {
            return statChanges.get(StatChange.HERO_DMG);
        }

        public Integer getAllCardsDamage() {
            return statChanges.get(StatChange.ALL_DMG);
        }

        public Integer getDamage() {
            return statChanges.get(StatChange.DMG);
        }
    }
}
