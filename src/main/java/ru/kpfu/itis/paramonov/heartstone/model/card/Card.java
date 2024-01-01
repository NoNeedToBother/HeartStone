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
        statuses.add(status);
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
