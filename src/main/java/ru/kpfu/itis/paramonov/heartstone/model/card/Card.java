package ru.kpfu.itis.paramonov.heartstone.model.card;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

public class Card implements Sprite{

    int hp;

    int atk;

    int cost;

    CardRepository.CardTemplate cardInfo;

    public Card(CardRepository.CardTemplate cardInfo) {
        this.cardInfo = cardInfo;

        this.hp = cardInfo.getHp();

        this.atk = cardInfo.getAtk();

        this.cost = cardInfo.getCost();
    }

    public static class CardSpriteBuilder implements SpriteBuilder<StackPane> {
        private StackPane sp = new StackPane();

        private final double CARD_WIDTH = 120.0;

        private final double CARD_HEIGHT = 160.0;

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
    }



    private static final CardSpriteBuilder builder = new CardSpriteBuilder();

    public static CardSpriteBuilder Builder() {
        return builder;
    }
}
