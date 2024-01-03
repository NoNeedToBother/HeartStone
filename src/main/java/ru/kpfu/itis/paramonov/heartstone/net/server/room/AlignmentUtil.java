package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.util.List;

public class AlignmentUtil {
    public static void onAlignedStatusApply(Card attacker, Card attacked, List<Card> attackedField, List<Integer> attackedIndexes,
                                            CardRepository.Status previous, CardRepository.Status alignment,
                                            GameServer.Client attackerPlayer, GameRoom room) {
        attacker.onApplyAlignedStatus(previous, alignment);
        if (previous.equals(CardRepository.Status.INTELLIGENCE)) {
            switch (alignment) {
                case ENERGY -> room.drawCard(attackerPlayer);
                case CHAOS -> {
                    attacked.decreaseHp(2);
                    int attackedIndex = attackedField.indexOf(attacked);
                    CardAttackUtil.decreaseHpFromNeighbourCard(1, attackedIndex - 1, attackedField, attackedIndexes);
                    CardAttackUtil.decreaseHpFromNeighbourCard(1, attackedIndex - 1, attackedField, attackedIndexes);
                }
            }
        }
        if (previous.equals(CardRepository.Status.STRENGTH) && alignment.equals(CardRepository.Status.INTELLIGENCE)) {
            for (int index = 0; index < attackedField.size(); index++) {
                attackedField.get(index).decreaseHp(1);
                if (!attackedIndexes.contains(index)) attackedIndexes.add(index);
            }
        }
    }

    public static CardRepository.Status getAlignment(Card attacker) {
        if (attacker.getCardInfo().getActions().contains(CardRepository.Action.DEAL_CHAOS_DMG)) return CardRepository.Status.CHAOS;
        else if (attacker.getCardInfo().getActions().contains(CardRepository.Action.DEAL_ENERGY_DMG)) return CardRepository.Status.ENERGY;
        else if (attacker.getCardInfo().getActions().contains(CardRepository.Action.DEAL_INTELLIGENCE_DMG)) return CardRepository.Status.INTELLIGENCE;
        else if (attacker.getCardInfo().getActions().contains(CardRepository.Action.DEAL_LIFE_DMG)) return CardRepository.Status.LIFE;
        else if (attacker.getCardInfo().getActions().contains(CardRepository.Action.DEAL_VOID_DMG)) return CardRepository.Status.VOID;
        else if (attacker.getCardInfo().getActions().contains(CardRepository.Action.DEAL_STRENGTH_DMG)) return CardRepository.Status.STRENGTH;
        else throw new RuntimeException("No alignment");
    }
}
