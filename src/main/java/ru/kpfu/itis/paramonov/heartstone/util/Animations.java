package ru.kpfu.itis.paramonov.heartstone.util;

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
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.controller.BattlefieldController;
import ru.kpfu.itis.paramonov.heartstone.controller.PacksController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

public class Animations {
    public static void playCardCrackingAnimation(ImageView iv, BattlefieldController controller) {
        final int FRAME_AMOUNT = 5;

        Runnable crackingCardAnim = () -> {
            for (int i = 1; i < FRAME_AMOUNT + 1; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                drawCardCrackingFrame(i, iv);
            }
            Platform.runLater(() -> controller.deleteCard(iv));
        };

        Thread thread = new Thread(crackingCardAnim);
        thread.start();
    }

    private static void drawCardCrackingFrame(int pos, ImageView iv) {
        BufferedImage img = SwingFXUtils.fromFXImage(iv.getImage(), null);
        img = BufferedImageUtil.addImage(img, "/assets/animations/card_cracking/card_cracking_" + pos + ".png");
        draw(img, iv);
    }

    public static void playHeroCrackingAnimation(ImageView iv, boolean win) {
        final int FRAME_AMOUNT = 3;

        Runnable crackingCardAnim = () -> {
            for (int i = 1; i < FRAME_AMOUNT + 1; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                drawHeroCrackingFrame(i, iv);
            }
            Platform.runLater(() -> {
                BattlefieldController.resetController();
                FXMLLoader loader = new FXMLLoader(GameApplication.class.getResource("/fxml/game_end.fxml"));
                try {
                    AnchorPane pane = loader.load();
                    Text title = new Text();
                    Font font = Font.loadFont(GameApplication.class.getResource("/fonts/ThaleahFat.ttf").toString(), 80);
                    title.setFont(font);
                    title.setX(675);
                    title.setY(125);
                    if (win) title.setText("You won!");
                    else title.setText("You lost!");
                    pane.getChildren().add(title);
                    Scene scene = new Scene(pane);
                    GameApplication.getApplication().getPrimaryStage().setScene(scene);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        };

        Thread thread = new Thread(crackingCardAnim);
        thread.start();
    }

    private static void drawHeroCrackingFrame(int pos, ImageView iv) {
        BufferedImage img = SwingFXUtils.fromFXImage(iv.getImage(), null);
        img = BufferedImageUtil.addImage(img, "/assets/animations/hero_cracking/hero_cracking_" + pos + ".png");
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

    public static void playCardAttacking(ImageView attacker, ImageView attacked) {
        AtomicReference<TranslateTransition> transition = new AtomicReference<>(new TranslateTransition());
        double attackerX = attacker.localToScene(attacker.getBoundsInLocal()).getCenterX();
        double attackerY = attacker.localToScene(attacker.getBoundsInLocal()).getCenterY();
        double attackedX = attacked.localToScene(attacked.getBoundsInLocal()).getCenterX();
        double attackedY = attacked.localToScene(attacked.getBoundsInLocal()).getCenterY();
        double deltaX = attackedX - attackerX;
        double deltaY = attackedY - attackerY;
        playTransition(transition, attacker, deltaX, deltaY, 400);
        transition.get().setOnFinished(actionEvent -> {
            playTransition(transition, attacker, -deltaX, -deltaY, 200);
            transition.get().setOnFinished(actionEvent1 -> {
                try {
                    BattlefieldController.getController().notifyAttackingAnimationStopped();
                } catch (NullPointerException ignored) {}
            });
        });
    }
}
