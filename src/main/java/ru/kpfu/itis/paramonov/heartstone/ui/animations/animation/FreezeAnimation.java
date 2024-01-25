package ru.kpfu.itis.paramonov.heartstone.ui.animations.animation;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.ui.animations.Animation;
import ru.kpfu.itis.paramonov.heartstone.ui.battle.BattleCard;
import ru.kpfu.itis.paramonov.heartstone.util.CardImages;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.List;

public class FreezeAnimation extends Animation {

    public enum Type {
        FREEZING, UNFREEZING
    }

    private final Type type;

    private BattleCard card;

    public FreezeAnimation(Type type, BattleCard card) {
        this.type = type;
        this.card = card;
    }

    private final static int FREEZING_FRAME_AMOUNT = 4;

    private final Runnable freezingCardAnimation = () -> {
        for (int i = 1; i <= FREEZING_FRAME_AMOUNT; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            drawFrame(i, "freezing/freezing_", card.getAssociatedImageView());
        }
        invokeOnAnimationEndedListeners();
    };

    private final Runnable unfreezingCardAnimation = () -> {
        for (int i = FREEZING_FRAME_AMOUNT - 1; i >= 1; i--) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Image base = CardImages.getPortraitWithStatusesAndEffects(card, List.of(CardRepository.Status.FROZEN));
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(base, null);
            ImageUtil.addImage(bufferedImage, getFreezingFrame(i));
            card.getAssociatedImageView().setImage(ImageUtil.toImage(bufferedImage));
        }
        Image base = CardImages.getPortraitWithStatusesAndEffects(card, List.of());
        card.getAssociatedImageView().setImage(base);
        invokeOnAnimationEndedListeners();
    };

    @Override
    public void play() {
        Thread thread = null;
        switch (type) {
            case FREEZING -> thread = new Thread(freezingCardAnimation);
            case UNFREEZING -> thread = new Thread(unfreezingCardAnimation);
        }
        thread.start();
    }

    public Image getFreezingFrame(int frameUntil) {
        BufferedImage bufferedImage = new BufferedImage(
                BattleCard.CardSpriteBuilder.DEFAULT_WIDTH, BattleCard.CardSpriteBuilder.DEFAULT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for(int i = 1; i <= frameUntil; i++) {
            ImageUtil.addImage(bufferedImage, DEFAULT_PATH + "freezing/freezing_" + i + ".png");
        }
        return ImageUtil.toImage(bufferedImage);
    }
}
