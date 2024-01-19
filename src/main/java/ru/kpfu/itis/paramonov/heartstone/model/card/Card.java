package ru.kpfu.itis.paramonov.heartstone.model.card;

import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Card {

    private int hp;

    private int maxHp;

    private int atk;

    private int cost;

    private List<CardRepository.Status> statuses;

    private CardRepository.CardTemplate cardInfo;

    public Card(CardRepository.CardTemplate cardInfo) {
        this.cardInfo = cardInfo;
        this.hp = cardInfo.getHp();
        this.maxHp = hp;
        this.atk = cardInfo.getAtk();
        this.cost = cardInfo.getCost();
        this.statuses = new ArrayList<>();
    }

    public Card(int id, int hp, int atk, int cost) {
        this.hp = hp;
        this.maxHp = hp;
        this.atk = atk;
        this.cost = cost;
        this.cardInfo = CardRepository.getCardTemplate(id);
        this.statuses = new ArrayList<>();
    }

    public Card() {}

    public int getHp() {
        return hp;
    }

    public int getAtk() {
        return atk;
    }

    public int getCost() {
        return cost;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setAtk(int atk) {
        this.atk = atk;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public CardRepository.CardTemplate getCardInfo() {
        return cardInfo;
    }

    public void decreaseHp(int hp) {
        if (statuses.contains(CardRepository.Status.SHIELDED)) {
            removeStatus(CardRepository.Status.SHIELDED);
            addStatus(CardRepository.Status.SHIELD_REMOVED_1);
        }
        else this.hp -= hp;
    }

    public void increaseHp(int hp) {
        this.hp += hp;
        if (this.hp > maxHp) this.hp = maxHp;
    }

    public void increaseMaxHp(int hp) {
        this.maxHp += hp;
        this.hp += hp;
    }

    public void decreaseCost(int cost) {
        this.cost -= cost;
    }

    public List<CardRepository.Status> getStatuses() {
        return statuses;
    }

    public List<CardRepository.Status> getNonUtilityStatuses() {
        List<CardRepository.Status> res = new ArrayList<>(statuses);
        return res.stream()
                .filter(status -> !status.isUtility())
                .collect(Collectors.toList());
    }

    public void addStatus(CardRepository.Status status) {
        statuses.remove(status);
        statuses.add(status);
    }
    public boolean hasStatus(CardRepository.Status status) {
        return statuses.contains(status);
    }

    public boolean hasAction(CardRepository.Action action) {
        return cardInfo.getActions().contains(action);
    }

    public boolean hasFaction(CardRepository.Faction faction) {
        return cardInfo.getFactions().contains(faction);
    }

    public boolean hasKeyWord(CardRepository.KeyWord keyWord) {
        return cardInfo.getKeyWords().contains(keyWord);
    }

    public void getDamageAndChangeAlignment(Integer hpDecrease, CardRepository.Status alignedStatusToRemove,
                                            CardRepository.Status newAlignedStatus) {
        if (hpDecrease != null) hp -= hpDecrease;
        changeAlignment(alignedStatusToRemove, newAlignedStatus);
    }

    public void changeStats(Integer atkIncrease, Integer hpHeal, Integer maxHpIncrease) {
        if (atkIncrease != null) atk += atkIncrease;
        if (hpHeal != null) increaseHp(hpHeal);
        if (maxHpIncrease != null) increaseMaxHp(maxHpIncrease);
    }

    public void changeAlignment(CardRepository.Status alignedStatusToRemove, CardRepository.Status newAlignedStatus) {
        if (alignedStatusToRemove != null) statuses.remove(alignedStatusToRemove);
        if (newAlignedStatus != null) statuses.add(newAlignedStatus);
    }

    public CardRepository.Status getCurrentAlignedStatus() {
        for (CardRepository.Status status : statuses) {
            if (status.isAlignmentStatus()) return status;
        }
        return null;
    }

    public void removeStatus(CardRepository.Status status) {
        statuses.remove(status);
    }
}
