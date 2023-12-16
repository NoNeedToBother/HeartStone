package ru.kpfu.itis.paramonov.heartstone.util;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
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

        Runnable crackingCardAnim = new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i < FRAME_AMOUNT + 1; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    drawCardCrackingFrame(i, iv);
                }
                Platform.runLater(() -> controller.deleteCard(iv));
            }
        };

        Thread thread = new Thread(crackingCardAnim);
        thread.start();
    }

    private static void drawCardCrackingFrame(int pos, ImageView iv) {
        BufferedImage img = SwingFXUtils.fromFXImage(iv.getImage(), null);
        img = BufferedImageUtil.addImage(img, "/assets/animations/card_cracking/card_cracking_" + pos + ".png");
        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "PNG", out);
            InputStream in = new ByteArrayInputStream(out.toByteArray());
            iv.setImage(new Image(in));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void playPackShakingAnimation(ImageView pack) {
        AtomicReference<TranslateTransition> transition = new AtomicReference<>(new TranslateTransition());
        transition.get().setNode(pack);
        transition.get().setByX(25);
        transition.get().setDuration(Duration.millis(150));
        transition.get().play();
        transition.get().setOnFinished(actionEvent -> {
            transition.set(new TranslateTransition());
            transition.get().setNode(pack);
            transition.get().setByX(-50);
            transition.get().setDuration(Duration.millis(150));
            transition.get().play();
            transition.get().setOnFinished(actionEvent1 -> {
                transition.set(new TranslateTransition());
                transition.get().setNode(pack);
                transition.get().setByX(75);
                transition.get().setDuration(Duration.millis(100));
                transition.get().play();
                transition.get().setOnFinished(actionEvent2 -> {
                    transition.set(new TranslateTransition());
                    transition.get().setNode(pack);
                    transition.get().setByX(-100);
                    transition.get().setDuration(Duration.millis(100));
                    transition.get().play();
                    transition.get().setOnFinished(actionEvent3 -> {
                        transition.set(new TranslateTransition());
                        transition.get().setNode(pack);
                        transition.get().setByX(50);
                        transition.get().setDuration(Duration.millis(75));
                        transition.get().play();
                        transition.get().setOnFinished(actionEvent4 -> {
                            PacksController.getController().notifyAnimationEnded();
                        });
                    });
                });
            });
        });
    }
}
