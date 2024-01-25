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
