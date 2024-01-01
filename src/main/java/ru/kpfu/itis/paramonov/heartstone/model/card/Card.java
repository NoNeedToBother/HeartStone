package ru.kpfu.itis.paramonov.heartstone.model.card;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.model.Sprite;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.util.BufferedImageUtil;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

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
        this.hp -= hp;
    }

    public void increaseHp(int hp) {
        this.hp += hp;
        if (hp > maxHp) this.hp = maxHp;
    }

    public void increaseMaxHp(int hp) {
        this.hp += hp;
    }

    public List<CardRepository.Status> getStatuses() {
        return statuses;
    }

    public void addStatus(CardRepository.Status status) {
        statuses.remove(status);
        if (status.isAlignmentStatus()) {
            onAlignmentStatusApplied(status);
        } else statuses.add(status);
    }

    private void onAlignmentStatusApplied(CardRepository.Status alignment) {
        CardRepository.Status currentAlignment = getCurrentAlignment();
        if (currentAlignment == null) {
            statuses.add(alignment);
            return;
        }
        if (currentAlignment.equals(alignment)) return;
        if (currentAlignment.equals(CardRepository.Status.VOID)) removeStatus(CardRepository.Status.VOID);
        if (alignment.equals(CardRepository.Status.VOID)) removeStatus(currentAlignment);
        switch (currentAlignment) {
            case STRENGTH -> {
                switch (alignment) {
                    case ENERGY -> changeStatsAndAlignment(null, null, 1,
                            CardRepository.Status.STRENGTH, CardRepository.Status.ENERGY);
                    case CHAOS, INTELLIGENCE -> changeAlignment(currentAlignment, alignment);
                    case LIFE -> changeAlignment(currentAlignment, null);
                }
            }
            case ENERGY -> {
                switch (alignment) {
                    case STRENGTH -> changeStatsAndAlignment(null, null, 1,
                            CardRepository.Status.ENERGY, alignment);
                    case LIFE -> changeStatsAndAlignment(null, null, 1,
                            CardRepository.Status.ENERGY, null);
                    case CHAOS -> changeStatsAndAlignment(null, null, 2,
                            CardRepository.Status.ENERGY, CardRepository.Status.CHAOS);
                    case INTELLIGENCE -> changeAlignment(currentAlignment, alignment);
                }
            }
            case CHAOS -> {
                switch (alignment) {
                    case ENERGY -> changeStatsAndAlignment(null, null, 2,
                            CardRepository.Status.CHAOS, CardRepository.Status.ENERGY);
                    case INTELLIGENCE -> changeAlignment(CardRepository.Status.CHAOS, CardRepository.Status.INTELLIGENCE);
                    case LIFE -> changeAlignment(CardRepository.Status.CHAOS, null);
                    case STRENGTH -> changeAlignment(currentAlignment, alignment);
                }
            }
            case INTELLIGENCE -> {
                switch (alignment) {
                    case CHAOS -> changeStatsAndAlignment(null, null, 1,
                            CardRepository.Status.INTELLIGENCE, null);
                    case STRENGTH -> changeAlignment(CardRepository.Status.INTELLIGENCE, null);
                    case LIFE -> changeAlignment(currentAlignment, alignment);
                }
            }
            case LIFE -> {
                switch (alignment) {
                    case STRENGTH -> changeAlignment(CardRepository.Status.LIFE, null);
                    case ENERGY -> changeStatsAndAlignment(null, null, 1,
                            CardRepository.Status.LIFE, null);
                    case INTELLIGENCE, CHAOS -> changeAlignment(currentAlignment, alignment);
                }
            }
        }
    }

    public void changeStatsAndAlignment(Integer atkIncrease, Integer hpIncrease, Integer hpDecrease,
                                        CardRepository.Status alignedStatusToRemove, CardRepository.Status newAlignedStatus) {
        if (atkIncrease != null) atk += atkIncrease;
        if (hpIncrease != null) {
            increaseMaxHp(hpIncrease);
            increaseHp(hpIncrease);
        }
        if (hpDecrease != null) hp -= hpDecrease;
        changeAlignment(alignedStatusToRemove, newAlignedStatus);
    }

    public void changeAlignment(CardRepository.Status alignedStatusToRemove, CardRepository.Status newAlignedStatus) {
        if (alignedStatusToRemove != null) statuses.remove(newAlignedStatus);
        if (newAlignedStatus != null) statuses.add(newAlignedStatus);
    }

    private CardRepository.Status getCurrentAlignment() {
        for (CardRepository.Status status : statuses) {
            if (status.isAlignmentStatus()) return status;
        }
        return null;
    }

    public void removeStatus(CardRepository.Status status) {
        statuses.remove(status);
    }

    public static class CardSpriteBuilder implements SpriteBuilder<Image> {
        private BufferedImage img = new BufferedImage(96, 128, BufferedImage.TYPE_INT_ARGB);

        private final String DEFAULT_PATH = "/assets/images/cards";

        private SpriteBuilder<Image> addImageToBufferedImage(String imgUrl) {
            img = BufferedImageUtil.addImage(img, imgUrl);
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
            img = BufferedImageUtil.scale(img, scale);
            return this;
        }

        @Override
        public Image build() {
            return BufferedImageUtil.toImage(img);
        }
    }

    public static CardSpriteBuilder spriteBuilder() {
        return new CardSpriteBuilder();
    }
}
