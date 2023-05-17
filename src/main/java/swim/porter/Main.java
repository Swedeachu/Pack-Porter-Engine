package swim.porter;

import org.apache.commons.io.FileUtils;
import swim.porter.engine.PortFileProcessor;
import swim.porter.engine.ZipUtil;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class Main {

  public static String portDir;

  static {
    try {
      File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
      portDir = Paths.get(jarFile.getParent(), "Pack Jar").toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    setUpAssets();
    String returnValue = "";  // The string value you want to return

    if (args.length > 0 && args[0].equalsIgnoreCase("port") && args.length > 1) {
      String arg = args[1];
      String portedPackPath = PortFileProcessor.handlePortCommand(arg);
      returnValue = portedPackPath;  // Set the return value
    } else {
      System.out.println("Proper command syntax is 'port url'\n Examples:\n port \"mediafire.com/file.zip\"\n port \"C:\\file.zip\"");
    }

    // Set the exit code
    // our server will have to parse the hash code back into a string and upload file back to client
    System.exit(returnValue.hashCode());
  }

  private static void setUpAssets() {
    try {
      File portBotDirFile = new File(portDir);
      File assetsDir = new File(portBotDirFile, "assets");
      // Check if we don't have an assets folder
      if (!assetsDir.exists()) {
        portBotDirFile.mkdirs();
        // If running from the JAR, copy the assets.zip to a temporary file and extract from there
        if (Main.class.getResource("/assets.zip").toString().startsWith("jar:")) {
          try (InputStream inputStream = Main.class.getResourceAsStream("/assets.zip")) {
            File tempAssetsZip = File.createTempFile("assets", ".zip");
            FileUtils.copyInputStreamToFile(inputStream, tempAssetsZip);
            ZipUtil.lightUnzip(tempAssetsZip, assetsDir);
            tempAssetsZip.delete();
          }
        } else {
          // If running from IDE or directly from the extracted JAR, extract assets.zip directly
          File assetsZipFile = new File(Main.class.getResource("/assets.zip").toURI());
          ZipUtil.lightUnzip(assetsZipFile, assetsDir);
        }
      }
      // need to clean up from any past uses
      PortFileProcessor.cleanPackDir();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
