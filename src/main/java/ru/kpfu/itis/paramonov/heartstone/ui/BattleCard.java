package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.model.Sprite;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.List;

public class BattleCard implements Sprite {
    Card card;

    private ImageView imageView;

    public enum CardStyle {
        BASE, SELECTED;
    }

    public BattleCard(Card card) {
        this.card = card;
    }

    public BattleCard(CardRepository.CardTemplate cardInfo) {
        card = new Card(cardInfo);
    }

    public BattleCard(int id, int hp, int atk, int cost) {
        card = new Card(id, hp, atk, cost);
    }

    public int getHp() {
        return card.getHp();
    }

    public int getAtk() {
        return card.getAtk();
    }

    public int getCost() {
        return card.getCost();
    }

    public void setHp(int hp) {
        card.setHp(hp);
    }

    public void setAtk(int atk) {
        card.setAtk(atk);
    }

    public void setCost(int cost) {
        card.setCost(cost);
    }

    public CardRepository.CardTemplate getCardInfo() {
        return card.getCardInfo();
    }

    public void removeStatus(CardRepository.Status status) {
        card.removeStatus(status);
    }

    public boolean hasFaction(CardRepository.Faction faction) {
        return card.hasFaction(faction);
    }

    public boolean hasKeyWord(CardRepository.KeyWord keyWord) {
        return card.hasKeyWord(keyWord);
    }

    public List<CardRepository.Status> getNonUtilityStatuses() {
        return card.getNonUtilityStatuses();
    }

    public void addStatus(CardRepository.Status status) {
        card.addStatus(status);
    }

    public boolean hasStatus(CardRepository.Status status) {
        return card.hasStatus(status);
    }

    public CardRepository.Status getCurrentAlignedStatus() {
        return card.getCurrentAlignedStatus();
    }



    public void associateImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public ImageView getAssociatedImageView() {
        return imageView;
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
