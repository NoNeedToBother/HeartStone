package ru.kpfu.itis.paramonov.heartstone.model.card;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.model.Sprite;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class Card implements Sprite {

    private int hp;

    private int maxHp;

    private int atk;

    private int cost;

    private List<CardRepository.Status> statuses;

    private CardRepository.CardTemplate cardInfo;

    private ImageView imageView;

    public enum CardStyle {
        BASE, SELECTED;
    }

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

    public void associateImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public ImageView getAssociatedImageView() {
        return imageView;
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

    public List<CardRepository.Status> getStatuses() {
        return statuses;
    }

    public List<CardRepository.Status> getNonUtilityStatuses() {
        List<CardRepository.Status> res = new ArrayList<>(statuses);
        return res.stream()
                .filter(status -> !status.isUtility())
                .collect(Collectors.toList());
    }

    public void addJustStatus(CardRepository.Status status) {
        statuses.remove(status);
        statuses.add(status);
    }

    public CardRepository.Status addStatus(CardRepository.Status status) {
        statuses.remove(status);
        if (status.isAlignmentStatus()) {
            return onAlignedStatusApplied(status);
        } else statuses.add(status);
        return status;
    }
    public boolean hasStatus(CardRepository.Status status) {
        for (CardRepository.Status cardStatus : statuses) {
            if (cardStatus.equals(status)) return true;
        }
        return false;
    }

    private CardRepository.Status onAlignedStatusApplied(CardRepository.Status alignment) {
        CardRepository.Status currentAlignment = getCurrentAlignedStatus();
        if (currentAlignment == null) {
            statuses.add(alignment);
            return alignment;
        }
        if (currentAlignment.equals(alignment)) return alignment;
        if (currentAlignment.equals(CardRepository.Status.VOID)) removeStatus(CardRepository.Status.VOID);
        if (alignment.equals(CardRepository.Status.VOID)) removeStatus(currentAlignment);
        switch (currentAlignment) {
            case STRENGTH -> {
                switch (alignment) {
                    case ENERGY -> {
                        return getDamageAndChangeAlignment(1, CardRepository.Status.STRENGTH, CardRepository.Status.ENERGY);
                    }
                    case CHAOS -> {
                        return changeAlignment(currentAlignment, alignment);
                    }
                    case LIFE, INTELLIGENCE -> {
                        return changeAlignment(currentAlignment, null);
                    }
                }
            }
            case ENERGY -> {
                switch (alignment) {
                    case STRENGTH -> {
                        return getDamageAndChangeAlignment(1, CardRepository.Status.ENERGY, alignment);
                    }
                    case LIFE -> {
                        return getDamageAndChangeAlignment(1, CardRepository.Status.ENERGY, null);
                    }
                    case CHAOS -> {
                        return getDamageAndChangeAlignment(2, CardRepository.Status.ENERGY, CardRepository.Status.CHAOS);
                    }
                    case INTELLIGENCE -> {
                        return changeAlignment(currentAlignment, alignment);
                    }
                }
            }
            case CHAOS -> {
                switch (alignment) {
                    case ENERGY -> {
                        return getDamageAndChangeAlignment(2, CardRepository.Status.CHAOS, CardRepository.Status.ENERGY);
                    }
                    case INTELLIGENCE, LIFE -> {
                        return changeAlignment(CardRepository.Status.CHAOS, null);
                    }
                    case STRENGTH -> {
                        return changeAlignment(currentAlignment, alignment);
                    }
                }
            }
            case INTELLIGENCE -> {
                switch (alignment) {
                    case CHAOS -> {
                        return getDamageAndChangeAlignment(2, CardRepository.Status.INTELLIGENCE, null);
                    }
                    case STRENGTH -> {
                        return changeAlignment(CardRepository.Status.INTELLIGENCE, null);
                    }
                    case LIFE -> {
                        return changeAlignment(currentAlignment, alignment);
                    }
                    case ENERGY -> {
                        return CardRepository.Status.INTELLIGENCE;
                    }
                }
            }
            case LIFE -> {
                switch (alignment) {
                    case STRENGTH -> {
                        return changeAlignment(CardRepository.Status.LIFE, null);
                    }
                    case ENERGY -> {
                        return getDamageAndChangeAlignment(1, CardRepository.Status.LIFE, null);
                    }
                    case INTELLIGENCE, CHAOS -> {
                        return changeAlignment(currentAlignment, alignment);
                    }
                }
            }
        }
        return null;
    }

    public void onApplyAlignedStatus(CardRepository.Status previousStatus, CardRepository.Status alignment) {
        if (previousStatus == null) return;
        switch (previousStatus) {
            case ENERGY -> {
                switch (alignment) {
                    case STRENGTH -> changeStats(1, null, null);
                    case LIFE -> changeStats(null, 2, null);
                }
            }
            case STRENGTH -> {
                switch (alignment) {
                    case LIFE -> changeStats(1, null, 1);
                    case ENERGY -> changeStats(1, null, null);
                }
            }
            case LIFE -> {
                switch (alignment) {
                    case STRENGTH -> changeStats(1, null, 1);
                    case ENERGY -> changeStats(null, 2, null);
                }
            }
            case INTELLIGENCE -> {
                switch (alignment) {
                    case STRENGTH -> changeStats(2, null, null);
                }
            }
            case CHAOS -> {
                switch (alignment) {
                    case INTELLIGENCE -> addStatus(CardRepository.Status.SHIELDED);
                    case LIFE -> changeStats(null, 1, 2);
                }
            }
        }
    }

    public CardRepository.Status getDamageAndChangeAlignment(Integer hpDecrease, CardRepository.Status alignedStatusToRemove,
                                                             CardRepository.Status newAlignedStatus) {
        if (hpDecrease != null) hp -= hpDecrease;
        changeAlignment(alignedStatusToRemove, newAlignedStatus);
        return newAlignedStatus;
    }

    public void changeStats(Integer atkIncrease, Integer hpHeal, Integer maxHpIncrease) {
        if (atkIncrease != null) atk += atkIncrease;
        if (hpHeal != null) increaseHp(hpHeal);
        if (maxHpIncrease != null) increaseMaxHp(maxHpIncrease);
    }

    public CardRepository.Status changeAlignment(CardRepository.Status alignedStatusToRemove, CardRepository.Status newAlignedStatus) {
        if (alignedStatusToRemove != null) statuses.remove(alignedStatusToRemove);
        if (newAlignedStatus != null) statuses.add(newAlignedStatus);
        return newAlignedStatus;
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

    public static class CardSpriteBuilder implements SpriteBuilder<Image> {
        public static final int DEFAULT_WIDTH = 96;

        public static final int DEFAULT_HEIGHT = 128;

        private BufferedImage img = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        private final String DEFAULT_PATH = "/assets/images/cards";

        private SpriteBuilder<Image> addImageToBufferedImage(String imgUrl) {
            img = ImageUtil.addImage(img, imgUrl);
            return this;
        }

        @Override
        public SpriteBuilder<Image> setStyle(String style) {
            switch (CardStyle.valueOf(style)) {
                case BASE -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/base_card.png");
                }
                case SELECTED -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/selected_card.png");
                }
                default -> {throw new RuntimeException("Impossible");}
            }
        }

        @Override
        public SpriteBuilder<Image> addImage(String imgUrl) {
            return addImageToBufferedImage(imgUrl);
        }

        @Override
        public SpriteBuilder<Image> addRarity(CardRepository.Rarity rarity) {
            switch (rarity) {
                case COMMON -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/rarities/rarity_common.png");
                }
                case RARE -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/rarities/rarity_rare.png");
                }
                case EPIC -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/rarities/rarity_epic.png");
                }
                case LEGENDARY -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/rarities/rarity_legendary.png");
                }
                default -> throw new RuntimeException("No such rarity");
            }
        }

        @Override
        public SpriteBuilder<Image> scale(double scale) {
            img = ImageUtil.scale(img, scale);
            return this;
        }

        @Override
        public Image build() {
            return ImageUtil.toImage(img);
        }
    }

    public static CardSpriteBuilder spriteBuilder() {
        return new CardSpriteBuilder();
    }
}
