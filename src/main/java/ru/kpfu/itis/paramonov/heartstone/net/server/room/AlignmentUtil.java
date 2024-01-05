package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.util.List;

public class AlignmentUtil {
    public static final int POWER_DMG = 1;
    public static final int MADNESS_DIRECT_DMG = 2;
    public static final int MADNESS_INDIRECT_DMG = 1;
    public static final int OVERSATURATION_DMG = 2;
    public static final int ENTROPY_DMG = 2;
    public static final int ENLIGHTMENT_DMG = 1;
    public static void onAlignedStatusApply(Card attacker, Card attacked, List<Card> attackedField, List<Integer> attackedIndexes,
                                            CardRepository.Status previous, CardRepository.Status alignment,
                                            GameServer.Client attackerPlayer, GameRoom room) {
        onApplyAlignedStatus(attacker, previous, alignment);
        onAlignedStatusApplied(attacked, alignment);
        if (previous.equals(CardRepository.Status.INTELLIGENCE)) {
            switch (alignment) {
                case ENERGY -> room.drawCard(attackerPlayer);
                case CHAOS -> {
                    attacked.decreaseHp(MADNESS_DIRECT_DMG);
                    int attackedIndex = attackedField.indexOf(attacked);
                    CardAttackUtil.decreaseHpFromNeighbourCards(MADNESS_INDIRECT_DMG, attackedIndex, attackedField, attackedIndexes);
                }
            }
        }
        if (previous.equals(CardRepository.Status.STRENGTH) && alignment.equals(CardRepository.Status.INTELLIGENCE)) {
            for (int index = 0; index < attackedField.size(); index++) {
                attackedField.get(index).decreaseHp(ENLIGHTMENT_DMG);
                if (!attackedIndexes.contains(index)) attackedIndexes.add(index);
            }
        }
    }

    public static CardRepository.Status getAlignment(Card attacker) {
        if (attacker.hasAction(CardRepository.Action.DEAL_CHAOS_DMG)) return CardRepository.Status.CHAOS;
        else if (attacker.hasAction(CardRepository.Action.DEAL_ENERGY_DMG)) return CardRepository.Status.ENERGY;
        else if (attacker.hasAction(CardRepository.Action.DEAL_INTELLIGENCE_DMG)) return CardRepository.Status.INTELLIGENCE;
        else if (attacker.hasAction(CardRepository.Action.DEAL_LIFE_DMG)) return CardRepository.Status.LIFE;
        else if (attacker.hasAction(CardRepository.Action.DEAL_VOID_DMG)) return CardRepository.Status.VOID;
        else if (attacker.hasAction(CardRepository.Action.DEAL_STRENGTH_DMG)) return CardRepository.Status.STRENGTH;
        else throw new RuntimeException("No alignment");
    }

    public static void onAlignedStatusApplied(Card applied, CardRepository.Status alignment) {
        CardRepository.Status currentAlignment = applied.getCurrentAlignedStatus();
        if (currentAlignment == null) {
            applied.addStatus(alignment);
            return;
        }
        if (currentAlignment.equals(alignment)) return;
        if (currentAlignment.equals(CardRepository.Status.VOID)) applied.removeStatus(CardRepository.Status.VOID);
        if (alignment.equals(CardRepository.Status.VOID)) applied.removeStatus(currentAlignment);
        switch (currentAlignment) {
            case STRENGTH -> {
                switch (alignment) {
                    case ENERGY ->
                            applied.getDamageAndChangeAlignment(POWER_DMG, CardRepository.Status.STRENGTH, CardRepository.Status.ENERGY);
                    case CHAOS -> applied.changeAlignment(currentAlignment, alignment);
                    case LIFE, INTELLIGENCE -> applied.changeAlignment(currentAlignment, null);
                }
            }
            case ENERGY -> {
                switch (alignment) {
                    case STRENGTH ->
                            applied.getDamageAndChangeAlignment(POWER_DMG, CardRepository.Status.ENERGY, alignment);
                    case LIFE ->
                            applied.getDamageAndChangeAlignment(OVERSATURATION_DMG, CardRepository.Status.ENERGY, null);
                    case CHAOS ->
                            applied.getDamageAndChangeAlignment(ENTROPY_DMG, CardRepository.Status.ENERGY, CardRepository.Status.CHAOS);
                    case INTELLIGENCE -> applied.changeAlignment(currentAlignment, alignment);
                }
            }
            case CHAOS -> {
                switch (alignment) {
                    case ENERGY ->
                            applied.getDamageAndChangeAlignment(ENTROPY_DMG, CardRepository.Status.CHAOS, CardRepository.Status.ENERGY);
                    case INTELLIGENCE, LIFE -> applied.changeAlignment(CardRepository.Status.CHAOS, null);
                    case STRENGTH -> applied.changeAlignment(currentAlignment, alignment);
                }
            }
            case INTELLIGENCE -> {
                switch (alignment) {
                    case CHAOS, STRENGTH ->
                            applied.changeAlignment(CardRepository.Status.INTELLIGENCE, null);
                    case LIFE -> applied.changeAlignment(currentAlignment, alignment);
                    case ENERGY -> {}
                }
            }
            case LIFE -> {
                switch (alignment) {
                    case STRENGTH -> applied.changeAlignment(CardRepository.Status.LIFE, null);
                    case ENERGY ->
                            applied.getDamageAndChangeAlignment(OVERSATURATION_DMG, CardRepository.Status.LIFE, null);
                    case INTELLIGENCE, CHAOS -> applied.changeAlignment(currentAlignment, alignment);
                }
            }
        }
    }

    public static void onApplyAlignedStatus(Card applier, CardRepository.Status previousStatus, CardRepository.Status alignment) {
        if (previousStatus == null) return;
        switch (previousStatus) {
            case ENERGY -> {
                switch (alignment) {
                    case STRENGTH -> applier.changeStats(1, null, null);
                    case LIFE -> applier.changeStats(null, 2, null);
                }
            }
            case STRENGTH -> {
                switch (alignment) {
                    case LIFE -> applier.changeStats(1, null, 1);
                    case ENERGY -> applier.changeStats(1, null, null);
                }
            }
            case LIFE -> {
                switch (alignment) {
                    case STRENGTH -> applier.changeStats(1, null, 1);
                    case ENERGY -> applier.changeStats(null, 2, null);
                }
            }
            case INTELLIGENCE -> {
                switch (alignment) {
                    case STRENGTH -> applier.changeStats(2, null, null);
                }
            }
            case CHAOS -> {
                switch (alignment) {
                    case INTELLIGENCE -> applier.addStatus(CardRepository.Status.SHIELDED);
                    case LIFE -> applier.changeStats(null, 1, 2);
                }
            }
        }
    }

    public static void addAlignment(Card card, CardRepository.Status alignment) {
        card.addStatus(alignment);
        onAlignedStatusApplied(card, alignment);
    }
}
