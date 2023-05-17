package swim.porter.engine;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Recolor {

  // tints a BufferedImage to a color
  public static BufferedImage tint(BufferedImage image, Color color) {
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        Color pixelColor = new Color(image.getRGB(x, y), true);
        int r = (pixelColor.getRed() + color.getRed()) / 2;
        int g = (pixelColor.getGreen() + color.getGreen()) / 2;
        int b = (pixelColor.getBlue() + color.getBlue()) / 2;
        int a = pixelColor.getAlpha();
        int rgba = (a << 24) | (r << 16) | (g << 8) | b;
        image.setRGB(x, y, rgba);
      }
    }
    return image;
  }

}
