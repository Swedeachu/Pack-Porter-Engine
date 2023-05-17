package swim.porter.engine;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CubeMapper {

  public static void CubeMapBuild(BufferedImage skyMap, String cubeMapPath) throws IOException {
    int x = skyMap.getWidth() / 3;
    int y = skyMap.getHeight() / 2;
    // top right
    writeCubeMapImage(skyMap.getSubimage(skyMap.getHeight(), 0, x, y), cubeMapPath + "\\cubemap_0.png", false);
    // bottom left
    writeCubeMapImage(skyMap.getSubimage(0, skyMap.getHeight() / 2, x, y), cubeMapPath + "\\cubemap_1.png", false);
    // bottom middle
    writeCubeMapImage(skyMap.getSubimage(skyMap.getWidth() / 3, skyMap.getHeight() / 2, x, y), cubeMapPath + "\\cubemap_2.png", false);
    // bottom right
    writeCubeMapImage(skyMap.getSubimage(skyMap.getHeight(), skyMap.getHeight() / 2, x, y), cubeMapPath + "\\cubemap_3.png", false);
    // top middle, does 180 degree flip
    writeCubeMapImage(skyMap.getSubimage(skyMap.getWidth() / 3, 0, x, y), cubeMapPath + "\\cubemap_4.png", true);
    // top left, does 180 degree flip
    writeCubeMapImage(skyMap.getSubimage(0, 0, x, y), cubeMapPath + "\\cubemap_5.png", true);
  }

  private static void writeCubeMapImage(BufferedImage image, String filePath, boolean flip) throws IOException {
    if (flip) {
      AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
      tx.translate(image.getWidth(null) * -1, image.getHeight(null) * -1);
      AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
      BufferedImage flippedImage = op.filter(image, null);
      ImageIO.write(flippedImage, "png", new File(filePath));
    } else {
      ImageIO.write(image, "png", new File(filePath));
    }
  }

}
