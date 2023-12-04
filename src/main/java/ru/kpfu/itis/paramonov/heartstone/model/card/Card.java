package ru.kpfu.itis.paramonov.heartstone.model.card;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageOutputStreamImpl;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class Card implements Sprite{

    private int hp;

    private int atk;

    private int cost;

    private Image image;

    private CardRepository.CardTemplate cardInfo;

    public Card(CardRepository.CardTemplate cardInfo) {
        this.cardInfo = cardInfo;

        this.hp = cardInfo.getHp();

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

    public CardRepository.CardTemplate getCardInfo() {
        return cardInfo;
    }

    /*
    public static class CardSpriteBuilder implements SpriteBuilder<StackPane> {
        private StackPane sp = new StackPane();

        private final double CARD_WIDTH = 96.0;

        private final double CARD_HEIGHT = 128.0;

        private SpriteBuilder<StackPane> addImageToSp(String imgUrl) {
            ImageView iv = new ImageView();
            iv.setImage(new Image(imgUrl));
            iv.setFitWidth(CARD_WIDTH);
            iv.setFitHeight(CARD_HEIGHT);
            sp.getChildren().add(iv);
            return this;
        }

        @Override
        public SpriteBuilder<StackPane> setBase() {
            return addImageToSp("/base_card.png");
        }

        @Override
        public SpriteBuilder<StackPane> addImage(String imgUrl) {
            return addImageToSp(imgUrl);
        }

        @Override
        public SpriteBuilder<StackPane> addRarity(CardRepository.Rarity rarity) {
            switch (rarity) {
                case COMMON -> {
                    return addImageToSp("/rarity_common.png");
                }
                case RARE -> {
                    return addImageToSp("/rarity_rare.png");
                }
                case EPIC -> {
                    return addImageToSp("/rarity_epic.png");
                }
                case LEGENDARY -> {
                    return addImageToSp("/rarity_legendary");
                }
                default -> throw new RuntimeException("Impossible");
            }
        }

        @Override
        public StackPane build() {
            return sp;
        }
    }*/

    public static class CardSpriteBuilder implements SpriteBuilder<Image> {
        private BufferedImage img;

        private final String DEFAULT_PATH = "D:/projects/HeartStone/src/main/resources";

        public CardSpriteBuilder() {
            img = new BufferedImage(48, 64, BufferedImage.TYPE_INT_ARGB);
        }

        private SpriteBuilder<Image> addImageToBufferedImage(String imgUrl) {
            try {
                BufferedImage bufferedImage = ImageIO.read(new File(imgUrl));
                Graphics g = img.getGraphics();
                g.drawImage(bufferedImage, 0, 0, null);
                g.dispose();
                return this;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
                    return addImageToBufferedImage(DEFAULT_PATH + "/rarity_common.png");
                }
                case RARE -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/rarity_rare.png");
                }
                case EPIC -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/rarity_epic.png");
                }
                case LEGENDARY -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/rarity_legendary.png");
                }
                default -> {
                    throw new RuntimeException("Impossible");
                }
            }
        }

        @Override
        public Image build() {
            try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(img, "PNG", out);
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                return new Image(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final CardSpriteBuilder builder = new CardSpriteBuilder();

    public static CardSpriteBuilder Builder() {
        return builder;
    }
}
