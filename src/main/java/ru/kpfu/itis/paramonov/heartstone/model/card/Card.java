package ru.kpfu.itis.paramonov.heartstone.model.card;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.model.Sprite;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.util.BufferedImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class Card implements Sprite, Serializable{

    private int hp;

    private int maxHp;

    private int atk;

    private int cost;

    private CardRepository.CardTemplate cardInfo;

    private ImageView imageView;

    public Card(CardRepository.CardTemplate cardInfo) {
        this.cardInfo = cardInfo;

        this.hp = cardInfo.getHp();

        this.maxHp = cardInfo.getHp();

        this.atk = cardInfo.getAtk();

        this.cost = cardInfo.getCost();
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

    public CardRepository.CardTemplate getCardInfo() {
        return cardInfo;
    }

    public void associateImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public ImageView getAssociatedImageView() {
        return imageView;
    }

    public static class CardSpriteBuilder implements SpriteBuilder<Image> {
        private BufferedImage img;

        private final String DEFAULT_PATH = "/assets/images/cards";

        public CardSpriteBuilder() {
            setBlankImg();
        }

        private void setBlankImg() {
            img = new BufferedImage(48, 64, BufferedImage.TYPE_INT_ARGB);
        }

        private SpriteBuilder<Image> addImageToBufferedImage(String imgUrl) {
            img = BufferedImageUtil.addImage(img, imgUrl);
            return this;
        }

        @Override
        public SpriteBuilder<Image> setBase() {
            return addImageToBufferedImage(DEFAULT_PATH + "/base_card.png");
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
                default -> {
                    throw new RuntimeException("No such rarity");
                }
            }
        }

        @Override
        public SpriteBuilder<Image> scale(int scale) {
            img = BufferedImageUtil.scale(img, scale);
            return this;
        }

        @Override
        public Image build() {
            try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(img, "PNG", out);
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                setBlankImg();
                return new Image(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final CardSpriteBuilder builder = new CardSpriteBuilder();

    public static CardSpriteBuilder SpriteBuilder() {
        return builder;
    }
}
