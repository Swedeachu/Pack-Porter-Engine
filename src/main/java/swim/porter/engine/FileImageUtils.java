package swim.porter.engine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FileImageUtils {

  // rescales an image file and writes it instantly to the disk
  public static void resizeImageWrite(File originalImagePath, File resizedImagePath, int rescaleWidth, int rescaleHeight, String format) {
    try {
      if (originalImagePath.exists()) {
        BufferedImage original = ImageIO.read(originalImagePath);
        BufferedImage resized = Rescale.nearestNeighborRescale(original, rescaleWidth, rescaleHeight);
        ImageIO.write(resized, format, resizedImagePath);
      }
    } catch (IOException e) {
      System.out.println("failed to resize image");
    }
  }

  // removes the useless alpha pixels in an image that interfere with directX's sprite rendering
  public static BufferedImage imageTransparencyFix(BufferedImage raw) {
    int WIDTH = raw.getWidth();
    int HEIGHT = raw.getHeight();
    BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
    int pixels[] = new int[WIDTH * HEIGHT];
    raw.getRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);
    for (int i = 0; i < pixels.length; i++) {
      int alpha = (pixels[i] & 0xff000000) >>> 24;
      if (pixels[i] >= alpha) {
        pixels[i] = 0x00ffffff;
      }
    }
    image.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);
    return image;
  }

}
