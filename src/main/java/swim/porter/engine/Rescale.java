package swim.porter.engine;

import java.awt.*;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class Rescale {

  // this is just the cheapest and easiest method to implement for quick rescaling, if users really want more customization they can download the software
  public static BufferedImage nearestNeighborRescale(BufferedImage originalImage, int targetWidth, int targetHeight) {
    BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, TYPE_INT_ARGB);
    Graphics2D g = resizedImage.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
    g.dispose();
    return resizedImage;
  }

}
