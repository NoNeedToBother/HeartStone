package ru.kpfu.itis.paramonov.heartstone.ui.animations.animation;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.ui.animations.Animation;
import ru.kpfu.itis.paramonov.heartstone.ui.battle.BattleCard;
import ru.kpfu.itis.paramonov.heartstone.util.CardImages;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CardAttackingAnimation extends Animation {
    @FunctionalInterface
    public interface OnCardReturnedListener {
        void onCardReturned();
    }

    private final List<OnCardReturnedListener> onCardReturnedListeners = new ArrayList<>();

    private final ImageView attacker;

    private final ImageView attacked;

    public CardAttackingAnimation(ImageView attacker, ImageView attacked) {
        this.attacker = attacker;
        this.attacked = attacked;
    }

    private CardEffect cardEffect;

    public static class CardEffect {
        public enum Type {
            CLAW
        }

        private Type type;

        private List<Integer> positions;

        private List<BattleCard> field;

        public CardEffect(Type type, List<Integer> positions, List<BattleCard> field) {
            this.type = type;
            this.positions = positions;
            this.field = field;
        }

        public void trigger() {
            for (Integer pos : positions) {
                BattleCard card = field.get(pos);
                Runnable anim = () -> {
                    ImageView iv = card.getAssociatedImageView();
                    switch (type) {
                        case CLAW -> drawFrame("card_effects/cut.png", iv);
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ignored) {}
                    if (card.getHp() > 0) iv.setImage(CardImages.getPortraitWithStatusesAndEffects(card, List.of()));
                };

                Thread thread = new Thread(anim);
                thread.start();
            }
        }
    }

    public void addEffect(CardEffect cardEffect) {
        this.cardEffect = cardEffect;
    }

    @Override
    public void play() {
        AtomicReference<TranslateTransition> transition = new AtomicReference<>(new TranslateTransition());
        double attackerX = attacker.localToScene(attacker.getBoundsInLocal()).getCenterX();
        double attackerY = attacker.localToScene(attacker.getBoundsInLocal()).getCenterY();
        double attackedX = attacked.localToScene(attacked.getBoundsInLocal()).getCenterX();
        double attackedY = attacked.localToScene(attacked.getBoundsInLocal()).getCenterY();
        double deltaX = attackedX - attackerX;
        double deltaY = attackedY - attackerY;
        playTransition(transition, attacker, deltaX, deltaY, 400);
        transition.get().setOnFinished(actionEvent -> {
            invokeOnAnimationEndedListeners();
            if (cardEffect != null) cardEffect.trigger();
            playTransition(transition, attacker, -deltaX, -deltaY, 200);
            transition.get().setOnFinished(actionEvent1 -> {
                invokeOnCardReturnedListeners();
            });
        });
    }

    public void addOnCardReturnedListener(OnCardReturnedListener listener) {
        onCardReturnedListeners.add(listener);
    }
    private void invokeOnCardReturnedListeners() {
        Platform.runLater(() -> {
            for (OnCardReturnedListener listener : onCardReturnedListeners) {
                listener.onCardReturned();
            }
        });
    }
}
