package ru.kpfu.itis.paramonov.heartstone.util;

import ru.kpfu.itis.paramonov.heartstone.GameApplication;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;

public class BufferedImageUtil {
    public static BufferedImage addImage(BufferedImage target, String src) {
        try {
            BufferedImage bufferedImage = ImageIO.read(GameApplication.class.getResource(src));
            Graphics g = target.getGraphics();
            g.drawImage(bufferedImage, 0, 0, null);
            g.dispose();
            return target;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage scale(BufferedImage target, int scale) {
        int width = target.getWidth() * scale;
        int height = target.getHeight() * scale;
        BufferedImage after = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        AffineTransform scaleInstance = AffineTransform.getScaleInstance(scale, scale);
        AffineTransformOp scaleOp
                = new AffineTransformOp(scaleInstance, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

        Graphics2D g2 = (Graphics2D) after.getGraphics();
        g2.drawImage(target, scaleOp, 0, 0);
        g2.dispose();
        target = after;
        return target;
    }
}
