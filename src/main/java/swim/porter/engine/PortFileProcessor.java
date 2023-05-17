package swim.porter.engine;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import swim.porter.Main;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class PortFileProcessor {

  final static String fs = File.separator;

  public static String handlePortCommand(String arg) {
    File pack = null;
    // Check if arg is a file path
    File fileCheck = new File(URLDecoder.decode(arg, StandardCharsets.UTF_8));
    if (fileCheck.exists()) {
      pack = fileCheck;
    } else if (arg.contains("mediafire.com")) { // else wise check if arg was a website url
      boolean isRar = arg.contains(".rar");
      pack = Downloader.downloadMediafire(arg, isRar);
    } else if (arg.contains("pvprp.com")) {
      pack = Downloader.downloadPVPRP(arg);
    }

    // now port it
    if (pack != null) {
      File portedPack = processPort(pack);
      if (portedPack != null) {
        return portedPack.getAbsolutePath();
      }
    }

    return "failed";
  }

  // takes in file for pack to port, returns path to the ported pack
  private static File processPort(File export) {
    File pack;

    if (!export.isDirectory()) {
      String ext = FilenameUtils.getExtension(export.getName());
      String extractionPath = Paths.get(Main.portDir, FilenameUtils.removeExtension(export.getName())).toString();
      if (ext.equalsIgnoreCase("zip")) {
        ZipUtil.lightUnzip(export, new File(extractionPath));
      } else {
        ZipUtil.SevenExtract(export.getAbsolutePath(), extractionPath);
      }
      pack = new File(FilenameUtils.removeExtension(extractionPath));
    } else {
      pack = export;
    }

    // find the pack.mcmeta, the folder it is located is the root dir of the pack
    File manifest = findManifestFile(pack);
    if (manifest.exists()) {
      return Port.port(new File(manifest.getParentFile().getAbsolutePath()));
    } else {
      System.out.println("Error: uploaded pack does not contain pack.mcmeta");
    }

    return null;
  }


  // iteratively searches the pack for the mcmeta file
  private static File findManifestFile(File dir) {
    File manifest = new File(dir, "pack.mcmeta");
    if (manifest.exists()) {
      return manifest;
    }
    File[] files = dir.listFiles();
    assert files != null;
    for (File file : files) {
      if (file.isDirectory()) {
        manifest = findManifestFile(file);
        if (manifest.exists()) {
          return manifest;
        }
      }
    }
    return manifest;
  }

  // delete everything in the pack dir except for the assets folder
  public static void cleanPackDir() {
    File swimServicesDir = new File(Main.portDir);
    File[] swimFiles = swimServicesDir.listFiles();
    assert swimFiles != null;
    for (File file : swimFiles) {
      if (!file.getName().equalsIgnoreCase("assets")) {
        try {
          FileUtils.forceDelete(file);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

}
