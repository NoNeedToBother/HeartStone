package ru.kpfu.itis.paramonov.heartstone.util;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.controller.BattlefieldController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Animations {
    public static void crackCard(ImageView iv, BattlefieldController controller) {
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
                    drawFrame(i, iv);
                }
                Platform.runLater(() -> controller.deleteCard(iv));
            }
        };

        Thread thread = new Thread(crackingCardAnim);
        thread.start();
    }

    private static void drawFrame(int pos, ImageView iv) {
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
}
