package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.controller.BattlefieldController;
import ru.kpfu.itis.paramonov.heartstone.controller.PacksController;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.ui.battle.BattleCard;
import ru.kpfu.itis.paramonov.heartstone.util.CardImages;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Animations {
    private static final String DEFAULT_PATH = "/assets/animations/";

    public static Image getFreezingFrame(int frameUntil) {
        BufferedImage bufferedImage = new BufferedImage(
                BattleCard.CardSpriteBuilder.DEFAULT_WIDTH, BattleCard.CardSpriteBuilder.DEFAULT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for(int i = 1; i <= frameUntil; i++) {
            ImageUtil.addImage(bufferedImage, DEFAULT_PATH + "freezing/freezing_" + i + ".png");
        }
        return ImageUtil.toImage(bufferedImage);
    }

    private static void drawFrame(int pos, String src, ImageView iv) {
        BufferedImage img = SwingFXUtils.fromFXImage(iv.getImage(), null);
        img = ImageUtil.addImage(img, DEFAULT_PATH + src + pos + ".png");
        draw(img, iv);
    }

    private static void drawFrame(String src, ImageView iv) {
        BufferedImage img = SwingFXUtils.fromFXImage(iv.getImage(), null);
        img = ImageUtil.addImage(img, DEFAULT_PATH + src);
        draw(img, iv);
    }

    private static void draw(BufferedImage img, ImageView iv) {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "PNG", out);
            InputStream in = new ByteArrayInputStream(out.toByteArray());
            iv.setImage(new Image(in));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void playPackShakingAnimation(ImageView pack, Integer cardId, JSONArray cardIds) {
        PacksController.getController().clearCardImageViews();
        AtomicReference<TranslateTransition> transition = new AtomicReference<>();
        playTransition(transition, pack, 25, 0, 150);
        transition.get().setOnFinished(actionEvent -> {
            playTransition(transition, pack, -50, 0, 150);
            transition.get().setOnFinished(actionEvent1 -> {
                playTransition(transition, pack, 75, 0, 100);
                transition.get().setOnFinished(actionEvent2 -> {
                    playTransition(transition, pack, -100, 0, 100);
                    transition.get().setOnFinished(actionEvent3 -> {
                        playTransition(transition, pack, 50, 0, 75);
                        transition.get().setOnFinished(actionEvent4 -> {
                            PacksController.getController().notifyAnimationEnded();
                            if (cardId != null) PacksController.getController().showCard(cardId);
                            else PacksController.getController().showCards(cardIds);
                        });
                    });
                });
            });
        });
    }

    private static void playTransition(AtomicReference<TranslateTransition> transition, ImageView iv,
                                       double deltaX, double deltaY, long millis) {
        transition.set(new TranslateTransition());
        transition.get().setNode(iv);
        transition.get().setByX(deltaX);
        transition.get().setByY(deltaY);
        transition.get().setDuration(Duration.millis(millis));
        transition.get().play();
    }

    public static void playCardAttacking(ImageView attacker, ImageView attacked, Consumer<JSONObject> onAnimationEnded, JSONObject jsonObject) {
        AtomicReference<TranslateTransition> transition = new AtomicReference<>(new TranslateTransition());
        double attackerX = attacker.localToScene(attacker.getBoundsInLocal()).getCenterX();
        double attackerY = attacker.localToScene(attacker.getBoundsInLocal()).getCenterY();
        double attackedX = attacked.localToScene(attacked.getBoundsInLocal()).getCenterX();
        double attackedY = attacked.localToScene(attacked.getBoundsInLocal()).getCenterY();
        double deltaX = attackedX - attackerX;
        double deltaY = attackedY - attackerY;
        playTransition(transition, attacker, deltaX, deltaY, 400);
        transition.get().setOnFinished(actionEvent -> {
            if (onAnimationEnded != null) onAnimationEnded.accept(jsonObject);
            playTransition(transition, attacker, -deltaX, -deltaY, 200);
            transition.get().setOnFinished(actionEvent1 -> {
                try {
                    BattlefieldController.getController().notifyAttackingAnimationStopped();
                } catch (NullPointerException ignored) {}
            });
        });
    }

    public static void playFieldFireAnimation(ImageView iv) {
        final int FRAME_AMOUNT = 5;
        Image base = iv.getImage();

        Runnable fieldFireAnim = () -> {
            for (int i = 1; i <= FRAME_AMOUNT; i++) {
                BufferedImage img = SwingFXUtils.fromFXImage(base, null);
                try {
                    Thread.sleep(75);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ImageUtil.addImage(img, new Image(GameApplication.class.getResource(DEFAULT_PATH + "fire_effect/field_fire_effect_" + i + ".png").toString()));
                iv.setImage(ImageUtil.toImage(img));
            }
            iv.setImage(base);
        };

        Thread thread = new Thread(fieldFireAnim);
        thread.start();
    }

    public static void playPunishmentAnimation(ImageView iv, int punishmentSrcId) {
        if (punishmentSrcId == CardRepository.CardTemplate.TheRock.getId()) {
            Image before = iv.getImage();
            Runnable runnable = () -> {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(iv.getImage(), null);
                ImageUtil.addImage(bufferedImage, DEFAULT_PATH + "card_effects/rock_punishment.png");
                iv.setImage(ImageUtil.toImage(bufferedImage));
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) {}
                iv.setImage(before);
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }
    }

    public static void playCutAttackAnimation(List<Integer> positions, List<BattleCard> field) {
        for (Integer pos : positions) {
            BattleCard card = field.get(pos);
            Runnable anim = () -> {
                ImageView iv = card.getAssociatedImageView();
                drawFrame("card_effects/cut.png", iv);
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
