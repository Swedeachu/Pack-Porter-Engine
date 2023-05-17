package swim.porter.engine;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import swim.porter.Main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;

public class Port {

  final static String fs = File.separator;

  public static File port(File texturePackFile) {
    String packPath = (Main.portDir + fs);
    String filePath = texturePackFile.getAbsolutePath();
    boolean failed = false;
    String originalDescription = "";
    try {
      File mcmeta = new File(texturePackFile + fs + "pack.mcmeta");
      Scanner reader = new Scanner(mcmeta);
      // this gets the packs description from the mcmeta file
      try {
        while (reader.hasNextLine()) {
          String data = reader.nextLine();
          if (data.contains("description")) {
            String[] splitName = data.split("\"", 4);
            if (splitName.length >= 4) { // hopefully this if check prevents [1]
              String desc = splitName[3]; // [1] POTENTIALLY HUGE ERROR this causes packs to screw up sometimes when they really shouldn't
              originalDescription = desc.replaceAll("\\\\", " "); // filter out escape chars
              originalDescription = originalDescription.replaceAll("\"", " "); // filter out quotes
              // there could possibly be more escape chars to worry about having to filter, might want to write regex for this
            }
          }
        }
        reader.close();
      } catch (Exception e) {
        System.out.println("failed to read pack data");
        e.printStackTrace();
        PortFileProcessor.cleanPackDir();
      }
    } catch (FileNotFoundException ex) {
      System.out.println(" Failed to port: " + filePath + "\n could not locate pack.mcmeta");
      ex.printStackTrace();
      PortFileProcessor.cleanPackDir();
    }

    String rawName = FilenameUtils.removeExtension(texturePackFile.getName());
    String packName = FilenameUtils.getName(rawName + " PORT");
    String description = originalDescription + "\\ndiscord.gg/swim | Ported with Swimfan72's Auto Port ";

    if (!failed) {
      String texturePackPath = packPath + packName;
      new File(texturePackPath).mkdirs(); // folder of pack
      new File(texturePackPath + fs + "textures").mkdirs(); // textures
      new File(texturePackPath + fs + "textures" + fs + "ui").mkdirs(); // ui

      createManifest(packName, rawName, description, packPath); // creates the manifest for the ported pack
      icon(texturePackFile, texturePackPath); // copies over the pack icon
      textures(texturePackFile, texturePackPath); // copies over textures folder
      blockItemFix(texturePackPath); // renames block directory to blocks if needed, same for items
      lowerCaseAll(new File(texturePackPath)); // lower cases all file and directory names recursively in the pack
      armor(texturePackPath); // ports over the armor for the pack
      painting(texturePackPath); // copies and renames over the paintings
      colorMap(texturePackFile, texturePackPath); // copies over the colormap
      font(texturePackFile, texturePackPath); // folder move + rename ascii to default8
      gui(texturePackPath);
      fire(texturePackPath); // copies over the fire sprite sheets
      environment(texturePackFile, texturePackPath); // copies over the environment folder
      sky(texturePackFile, texturePackPath); // ports the java sky to a cubemap
      xp(texturePackPath); // ports over the xp bar from the gui
      guiFix(texturePackPath); // resizes accordingly
      crossHairFix(texturePackPath); // gets the crosshair from the gui and cleans the background
      chestFix(texturePackPath); // renames chests
      potionEffectsUI(texturePackPath); // extracts out the potion effect UI
      panorama(texturePackFile, texturePackPath); // if possible set a menu background for the pack
      sounds(texturePackFile, texturePackPath); // copy over the sounds files
      itemsFix(texturePackPath); // delete the weird alpha pixels from every item
      potions(texturePackPath); // iteratively create the potions
      containerUI(texturePackFile, packPath, packName); // ports containers UI
      grassSide(texturePackPath); // tga handling (broken)
      mobileButtons(texturePackPath); // creates default mobile buttons for the pack

      // finally we will need to zip the pack and turn its file extension to a .mcpack
      File mcpack = new File(texturePackPath + ".mcpack");
      finalizePort(texturePackPath);
      return (mcpack);
    }
    return null;
  }

  private static void createManifest(String PackName, String rawName, String description, String packPath) {
    try {
      FileWriter manifest = new FileWriter(packPath + PackName + fs + "manifest.json");
      // create the UUIDs for the manifest file
      String uuid = UUID.randomUUID().toString();
      String uuid2 = UUID.randomUUID().toString();
      manifest.write("{\n");
      manifest.write("    \"format_version\": 1,\n");
      manifest.write("    \"header\": {\n");
      manifest.write("        \"description\": \"" + description + "\",\n");
      manifest.write("        \"name\": \"" + rawName + "\",\n");
      manifest.write("        \"uuid\": \"" + uuid + "\",\n");
      manifest.write("        \"version\": [1, 0, 0],\n");
      manifest.write("        \"min_engine_version\": [1, 12, 1]\n");
      manifest.write("    },\n");
      manifest.write("    \"modules\": [\n");
      manifest.write("        {\n");
      manifest.write("            \"description\": \"" + description + "\",\n");
      manifest.write("            \"type\": \"resources\",\n");
      manifest.write("            \"uuid\": \"" + uuid2 + "\",\n");
      manifest.write("            \"version\": [1, 0, 0]\n");
      manifest.write("        }\n");
      manifest.write("    ]\n");
      manifest.write("}\n");
      manifest.close();
    } catch (IOException e) {
      System.out.println("An error occurred creating the manifest.json");
      e.printStackTrace();
      PortFileProcessor.cleanPackDir();
    }
  }

  private static void guiFix(String packPath) {
    try {
      File gui = new File(packPath + fs + "textures" + fs + "gui" + fs + "gui.png");
      if (gui.exists()) {
        BufferedImage guiIMG = ImageIO.read(gui);
        if (guiIMG.getHeight() != 256) {
          FileImageUtils.resizeImageWrite(gui, gui, 256, 256, "png");
        }
      }
    } catch (Exception e) {
      System.out.println("failed to scale gui");
    }
  }

  private static void icon(File file, String packPath) {
    File source = new File(file + fs + "pack.png");
    if (source.exists()) {
      File dest = new File(packPath + fs + "pack.png");
      try {
        FileUtils.moveFile(source, dest); // was originally copy
        File newIconName = new File(packPath + fs + "pack.png");
        File fixedName = new File(packPath + fs + "pack_icon.png");
        newIconName.renameTo(fixedName);
      } catch (IOException e) {
        System.out.println("icon error " + e.getMessage());
      }
    }
  }

  private static void chestFix(String packPath) {
    File chestOld = new File(packPath + fs + "textures" + fs + "entity" + fs + "chest" + fs + "normal_double.png");
    if (chestOld.exists()) {
      File chestNew = new File(packPath + fs + "textures" + fs + "entity" + fs + "chest" + fs + "double_normal.png");
      try {
        chestOld.renameTo(chestNew);
      } catch (Exception e) {
        System.out.println("failed to port chests: " + e.getMessage());
      }
    }
  }

  private static void textures(File file, String packPath) {
    File source = new File(file + fs + "assets" + fs + "minecraft" + fs + "textures");
    if (source.exists()) {
      File dest = new File(packPath + fs + "textures");
      try {
        FileUtils.copyDirectory(source, dest);
        //FileUtils.moveDirectory(source, dest);
      } catch (IOException e) {
        e.printStackTrace();
        System.out.println("failed to port textures: " + e.getMessage());
      }
    }
  }

  private static void mobileButtons(String packPath) {
    try {
      File guiURL = new File(packPath + fs + "textures" + fs + "gui" + fs + "gui.png");
      if (guiURL.exists()) {
        File mobileURL = new File(Main.portDir + fs + "assets" + fs + "mobile" + fs + "mobile_buttons.png");
        BufferedImage guiOriginal = ImageIO.read(guiURL);
        BufferedImage mobileImage = ImageIO.read(mobileURL);
        Graphics g = guiOriginal.getGraphics();
        g.drawImage(mobileImage, 0, 0, null);
        g.dispose();
        ImageIO.write(guiOriginal, "png", guiURL);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void grassSide(String packPath) {
    // we can only do this if we have the dirt block in the pack
    File dirt = new File(packPath + fs + "textures" + fs + "blocks" + fs + "dirt.png");
    if (dirt.exists()) {
      // get the grass side overlay
      File grassSide = new File(packPath + fs + "textures" + fs + "blocks" + fs + "grass_side_overlay.png");
      // we can only do this if grass side exists
      if (grassSide.exists()) {
        // make the grass side overlay
        sideOverlayTGA(packPath + fs + "textures" + fs + "blocks" + fs + "grass_side.tga", grassSide, dirt);
        // remove the old one
        File grass_side = new File(packPath + fs + "textures" + fs + "blocks" + fs + "grass_side.png");
        if (grass_side.exists()) {
          grass_side.delete();
        }
        // make the snow side overlay
        sideOverlayTGA(packPath + fs + "textures" + fs + "blocks" + fs + "grass_side_snow.tga", grassSide, dirt);
        // remove the old one
        File snowSide = new File(packPath + fs + "textures" + fs + "blocks" + fs + "grass_side_snowed.png");
        if (snowSide.exists()) {
          snowSide.delete();
        }
      }
    }
  }

  private static void sideOverlayTGA(String exportPath, File overlay, File base) {
    try {
      BufferedImage overlayImage = ImageIO.read(overlay);
      BufferedImage baseImage = ImageIO.read(base);
      if (overlayImage != null && baseImage != null) {
        int width = baseImage.getWidth();
        int height = baseImage.getHeight();
        // draw the images on top of each other with the baseImage having 50 alpha value
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        // Set the alpha value for the base image to be 20% opaque
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        graphics.drawImage(baseImage, 0, 0, null);
        // Scale and draw the grass overlay image on top of the base image
        BufferedImage scaledGrassSideImage = Rescale.nearestNeighborRescale(overlayImage, width, height);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // set transparency back to full
        graphics.drawImage(scaledGrassSideImage, 0, 0, null);
        // now write it to Targa format
        File outputImageFile = new File(exportPath);
        ImageIO.write(canvas, "tga", outputImageFile); // 12monkeys dependency should handle this
        graphics.dispose(); // close resources after use!
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // works recursively
  private static void lowerCaseAll(File master) {
    try {
      File[] directoryListing = master.listFiles();
      if (directoryListing != null) {
        for (File currentFile : directoryListing) {
          String lowerCasedName = FilenameUtils.getName(currentFile.toString().toLowerCase());
          File newName = new File(currentFile.getParentFile() + fs + lowerCasedName);
          currentFile.renameTo(newName);
          if (currentFile.isDirectory()) {
            lowerCaseAll(currentFile);
          }
        }
      }
    } catch (Exception e) {
      System.out.println("failed to lower case files: " + e.getMessage());
    }
  }

  private static void blockItemFix(String packPath) {
    try {
      File blockPath = new File(packPath + fs + "textures" + fs + "block" + fs);
      File blockPathFixed = new File(packPath + fs + "textures" + fs + "blocks" + fs);
      if (blockPath.exists()) {
        blockPath.renameTo(blockPathFixed);
      }
      File itemPath = new File(packPath + fs + "textures" + fs + "item" + fs);
      File itemPathFixed = new File(packPath + fs + "textures" + fs + "items" + fs);
      if (itemPath.exists()) {
        itemPath.renameTo(itemPathFixed);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("directory renaming not required for this pack?");
    }
  }

  private static void potionEffectsUI(String packPath) {
    try {
      File inventory = new File(packPath + fs + "textures" + fs + "gui" + fs + "container" + fs + "inventory.png");
      if (inventory.exists()) {
        String path = packPath + fs + "textures" + fs + "ui" + fs;
        BufferedImage spriteSheet = ImageIO.read(inventory);
        double sin = spriteSheet.getHeight() / 4.41379310345;
        int epilsonFlat = (int) Math.round(sin);
        int startingY = spriteSheet.getHeight() - epilsonFlat;
        double hypo = spriteSheet.getHeight() / 14.2222222222;
        int cellChangeFactor = (int) Math.round(hypo);
        String[] effects = {"speed", "slowness", "haste", "mining_fatigue", "strength", "weakness", "poison", "regeneration", "invisibility", "saturation", "jump_boost", "nausea", "night_vision", "blindness", "resistance", "fire_resistance", "water_breathing", "wither", "absorption"};
        // now that we have all our variables, loop through and sub image
        int sheetRow = 0;
        int x;
        for (int i = 0; i < 19; i++) {
          if (i % 8 == 0 && i != 0) {
            x = 0;
            sheetRow = 1;
            startingY = startingY + cellChangeFactor;
          } else {
            x = sheetRow * cellChangeFactor;
            sheetRow++;
          }
          ImageIO.write(spriteSheet.getSubimage(x, startingY, cellChangeFactor, cellChangeFactor), "png", new File(path + effects[i] + "_effect.png"));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static final Map<String, Color> PotionColorMap = new HashMap<>();

  static {
    PotionColorMap.put("blindness", Color.darkGray);
    PotionColorMap.put("damageBoost", new Color(139, 0, 0));
    PotionColorMap.put("fireResistance", Color.orange);
    PotionColorMap.put("harm", new Color(139, 0, 0));
    PotionColorMap.put("heal", Color.RED);
    PotionColorMap.put("invisibility", Color.lightGray);
    PotionColorMap.put("jump", Color.green);
    PotionColorMap.put("luck", Color.GREEN);
    PotionColorMap.put("moveSlowdown", Color.lightGray);
    PotionColorMap.put("moveSpeed", Color.CYAN);
    PotionColorMap.put("nightVision", Color.blue);
    PotionColorMap.put("poison", Color.green);
    PotionColorMap.put("regeneration", Color.PINK);
    PotionColorMap.put("slowFall", Color.LIGHT_GRAY);
    PotionColorMap.put("turtleMaster", Color.lightGray);
    PotionColorMap.put("waterBreathing", Color.lightGray);
    PotionColorMap.put("weakness", Color.black);
    PotionColorMap.put("haste", Color.yellow);
    PotionColorMap.put("wither", new Color(61, 43, 31));
  }

  private static void tintPots(String packPath, File potBlank, File overlay, boolean splash) {
    try {
      BufferedImage overImage = ImageIO.read(overlay);
      BufferedImage sourceImage = ImageIO.read(potBlank);
      // iterate the color map for each potion
      for (Map.Entry<String, Color> entry : PotionColorMap.entrySet()) {
        String effect = entry.getKey();
        Color color = entry.getValue();
        // we need to copy in a new image buffer
        BufferedImage source = new BufferedImage(sourceImage.getColorModel(), sourceImage.copyData(null), sourceImage.isAlphaPremultiplied(), null);
        BufferedImage over = new BufferedImage(overImage.getColorModel(), overImage.copyData(null), overImage.isAlphaPremultiplied(), null);
        // tint it and draw it in graphics image overlayed on top
        over = Recolor.tint(over, color);
        Graphics g = source.getGraphics();
        g.drawImage(over, 0, 0, null);
        g.dispose();
        // write to disk
        File path = new File(packPath + fs + "textures" + fs + "items" + fs + "potion_bottle_" + (splash ? "splash_" : "") + effect + ".png");
        ImageIO.write(source, "png", path);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void potions(String packPath) {
    try {
      File drinkBlank = new File(packPath + fs + "textures" + fs + "items" + fs + "potion_bottle_drinkable.png");
      File splashBlank = new File(packPath + fs + "textures" + fs + "items" + fs + "potion_bottle_splash.png");
      File overlay = new File(packPath + fs + "textures" + fs + "items" + fs + "potion_overlay.png");
      // check if our overlay png exists
      if (overlay.exists()) {
        // tint the drink pots
        if (drinkBlank.exists()) {
          BufferedImage overlayCompare = ImageIO.read(overlay);
          BufferedImage drinkCompare = ImageIO.read(drinkBlank);
          if (overlayCompare.getHeight() != drinkCompare.getHeight()) {
            // correcting the potion overlay size (drink)
            FileImageUtils.resizeImageWrite(overlay, overlay, drinkCompare.getHeight(), drinkCompare.getWidth(), "png");
          }
          tintPots(packPath, drinkBlank, overlay, false);
        }
        // tint the splash pots
        if (splashBlank.exists()) {
          BufferedImage overlayCompare = ImageIO.read(overlay);
          BufferedImage splashCompare = ImageIO.read(splashBlank);
          if (overlayCompare.getHeight() != splashCompare.getHeight()) {
            // correcting the potion overlay size (splash)
            FileImageUtils.resizeImageWrite(overlay, overlay, splashCompare.getWidth(), splashCompare.getHeight(), "png");
          }
          tintPots(packPath, splashBlank, overlay, true);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void itemsFix(String packPath) {
    try {
      File items = new File(packPath + fs + "textures" + fs + "items");
      if (items.exists()) {
        String[] images = items.list();
        assert images != null;
        for (String currentImage : images) {
          try {
            if (currentImage.toLowerCase().endsWith(".png")) {
              File img = new File(packPath + fs + "textures" + fs + "items" + fs + currentImage);
              BufferedImage item = ImageIO.read(img);
              BufferedImage clean = FileImageUtils.imageTransparencyFix(item);
              ImageIO.write(clean, "png", img);
            }
          } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("failed to fix image " + currentImage + " (might not be a .png file?)");
          }
        }
      }
    } catch (Exception e) {
      System.out.println("failed to find items folder");
    }
  }

  private static void xp(String packPath) {
    try {
      String path = packPath + fs + "textures" + fs + "ui" + fs;
      File iconsFile = new File(packPath + fs + "textures" + fs + "gui" + fs + "icons.png");
      if (iconsFile.exists()) {
        BufferedImage icons = ImageIO.read(iconsFile);
        int x = 0;
        int y = icons.getHeight() / 4;
        double padding = icons.getWidth() / 3.45945945946; // calculate extra space between the xp bar and img width
        int extra = (int) Math.round(padding); // round it to a whole integer
        int xpBarLength = icons.getWidth() - extra; // subtract the extra white space to find the xp bar's length
        // 256x256 is probably the smallest icons.png can be
        int xpBarWidth = 5 * (icons.getWidth() / 256); // height doubles as a multiple of 5, factor of width

        ImageIO.write(icons.getSubimage(x, y, xpBarLength, xpBarWidth), "png", new File(path + "experiencebarempty.png"));
        ImageIO.write(icons.getSubimage(x, y + xpBarWidth, xpBarLength, xpBarWidth), "png", new File(path + "experiencebarfull.png"));

        File source = new File(packPath + fs + "textures" + fs + "ui");
        File dest = new File(packPath + fs + "textures" + fs + "gui" + fs + "achievements");

        FileUtils.copyDirectory(source, dest);
        File xpBarEmpty2 = new File(packPath + fs + "textures" + fs + "gui" + fs + "achievements" + fs + "experiencebarempty.png");
        File xpBarFull2 = new File(packPath + fs + "textures" + fs + "gui" + fs + "achievements" + fs + "experiencebarfull.png");

        FileUtils.copyFile(xpBarEmpty2, new File(packPath + fs + "textures" + fs + "ui" + fs + "empty_progress_bar.png")); // new
        FileUtils.copyFile(xpBarFull2, new File(packPath + fs + "textures" + fs + "ui" + fs + "filled_progress_bar.png")); // new

        File hotdogempty = new File(packPath + fs + "textures" + fs + "gui" + fs + "achievements" + fs + "hotdogempty.png");
        File hotdogfull = new File(packPath + fs + "textures" + fs + "gui" + fs + "achievements" + fs + "hotdogfull.png");

        xpBarEmpty2.renameTo(hotdogempty);
        xpBarFull2.renameTo(hotdogfull);

        // now we need to put in notnub.png and nub.png into the achievements folder
        BufferedImage nubImage = ImageIO.read(new File(Main.portDir + fs + "assets" + fs + "nub" + fs + "nub.png"));
        File nubPath = new File(packPath + fs + "textures" + fs + "gui" + fs + "achievements" + fs + "nub.png");

        BufferedImage experienceNubImage = ImageIO.read(new File
                (Main.portDir + fs + "assets" + fs + "nub" + fs + "experiencenub.png"));
        File xpNubPath = new File(packPath + fs + "textures" + fs + "ui" + fs + "experiencenub.png");

        BufferedImage experienceBarBlueNubImage = ImageIO.read(new File(Main.portDir + fs + "assets" + fs + "nub" + fs + "experience_bar_nub_blue.png"));
        File xpBarNubPath = new File(packPath + fs + "textures" + fs + "ui" + fs + "experience_bar_nub_blue.png");
        if (new File(packPath + fs + "textures" + fs + "gui" + fs + "achievements").exists()) {
          ImageIO.write(nubImage, "png", nubPath);
          ImageIO.write(experienceNubImage, "png", xpNubPath);
          ImageIO.write(experienceBarBlueNubImage, "png", xpBarNubPath);
        }

        // a ghost achievements folder gets generated *sometimes* so I guess just delete it
        File ghost = new File(packPath + fs + "textures" + fs + "gui" + fs + "achievements" + fs + "achievements");
        if (ghost.exists()) {
          ghost.delete();
        }
        // now we need to make the xp bar json files
        // which will be done in barJson();
        barJson(packPath);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void barJson(String packPath) {
    try {
      FileWriter experiencebarempty = new FileWriter(packPath + fs + "textures" + fs + "ui" + fs + "experiencebarempty.json");
      experiencebarempty.write("{\n");
      experiencebarempty.write("  \"nineslice_size\": [\n");
      experiencebarempty.write("    6,\n");
      experiencebarempty.write("    1,\n");
      experiencebarempty.write("    6,\n");
      experiencebarempty.write("    1\n");
      experiencebarempty.write("  ],\n");
      experiencebarempty.write("  \"base_size\": [\n");
      experiencebarempty.write("    " + 182 + ",\n");
      experiencebarempty.write("    " + 5 + "\n");
      experiencebarempty.write("  ]\n");
      experiencebarempty.write("}\n");
      experiencebarempty.close();
    } catch (IOException e) {
      // System.out.println("An error occurred creating the experiencebarempty.json");
    }
    try {
      FileWriter experiencebarfull = new FileWriter(packPath + fs + "textures" + fs + "ui" + fs + "experiencebarfull.json");
      experiencebarfull.write("{\n");
      experiencebarfull.write("  \"nineslice_size\": [\n");
      experiencebarfull.write("    1,\n");
      experiencebarfull.write("    0,\n");
      experiencebarfull.write("    1,\n");
      experiencebarfull.write("    0\n");
      experiencebarfull.write("  ],\n");
      experiencebarfull.write("  \"base_size\": [\n");
      experiencebarfull.write("    " + 182 + ",\n");
      experiencebarfull.write("    " + 5 + "\n");
      experiencebarfull.write("  ]\n");
      experiencebarfull.write("}\n");
      experiencebarfull.close();
      // new
      File xpJSON = new File(packPath + fs + "textures" + fs + "ui" + fs + "experiencebarfull.json");
      FileUtils.copyFile(xpJSON, new File(packPath + fs + "textures" + fs + "gui" + fs + "achievements" + fs + "hotdogempty.json"));
      FileUtils.copyFile(xpJSON, new File(packPath + fs + "textures" + fs + "gui" + fs + "achievements" + fs + "hotdogfull.json"));
      FileUtils.copyFile(xpJSON, new File(packPath + fs + "textures" + fs + "ui" + fs + "empty_progress_bar.json"));
      FileUtils.copyFile(xpJSON, new File(packPath + fs + "textures" + fs + "ui" + fs + "filled_progress_bar.json"));
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("An error occurred creating the experiencebarfull.json");
    }
  }

  private static void sky(File file, String packPath) {
    String[] skyboxNames = {"cloud1", "cloud2", "starfield03", "starfield", "skybox", "skybox2"};
    BufferedImage skyMap = null;
    for (String skyboxName : skyboxNames) {
      File skyPath = new File(file + fs + "assets" + fs + "minecraft" + fs + "mcpatcher" + fs + "sky" + fs + "world0" + fs + skyboxName + ".png");
      if (skyPath.exists()) {
        try {
          skyMap = ImageIO.read(skyPath);
          break;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    // hopefully that managed to grab a sky box
    if (skyMap != null) {
      String path = packPath + fs + "textures" + fs + "environment" + fs + "overworld_cubemap" + fs;
      try {
        CubeMapper.CubeMapBuild(skyMap, path);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static void panorama(File file, String packPath) {
    try {
      File panorama = new File(file + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "gui" + fs + "title" + fs + "background" + fs);
      if (panorama.exists()) {
        FileUtils.moveDirectoryToDirectory(panorama, new File(packPath + fs + "textures" + fs + "gui" + fs), true);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void sounds(File file, String packPath) {
    try {
      File soundsDir = new File(file + fs + "assets" + fs + "minecraft" + fs + "sounds");
      if (soundsDir.exists()) {
        File dest = new File(packPath);
        // FileUtils.copyDirectoryToDirectory(soundsDir, dest);
        FileUtils.moveDirectoryToDirectory(soundsDir, dest, true);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static HashMap<String, String> armorNames = new HashMap<>();

  static {
    armorNames.put("diamond_layer_1", "diamond_1");
    armorNames.put("diamond_layer_2", "diamond_2");
    armorNames.put("chainmail_layer_1", "chain_1");
    armorNames.put("chainmail_layer_2", "chain_2");
    armorNames.put("gold_layer_1", "gold_1");
    armorNames.put("gold_layer_2", "gold_2");
    armorNames.put("iron_layer_1", "iron_1");
    armorNames.put("iron_layer_2", "iron_2");
    armorNames.put("leather_layer_1", "cloth_1");
    armorNames.put("leather_layer_2", "cloth_2");
  }

  private static void armor(String packPath) {
    String path = packPath + fs + "textures" + fs + "models" + fs + "armor" + fs;
    if (new File(path).exists()) {
      for (Map.Entry<String, String> entry : armorNames.entrySet()) {
        try {
          File original = new File(path + entry.getKey() + ".png");
          if (original.exists()) {
            File newName = new File(path + entry.getValue() + ".png");
            original.renameTo(newName);
          }
        } catch (Exception e) {
          System.out.println("error renaming armor");
        }
      }
    }
  }

  private static void colorMap(File file, String packPath) {
    try {
      File source = new File(file + fs + "assets" + fs + "minecraft" + fs + "mcpatcher" + fs + "colormap");
      if (source.exists()) {
        File dest = new File(packPath + fs + "textures" + fs + "colormap");
        FileUtils.moveDirectory(source, dest); // was originally copy
      }
    } catch (Exception e) {
      System.out.println("failed to locate colormap");
    }
  }

  private static void environment(File file, String packPath) {
    try {
      new File(packPath + fs + "textures" + fs + "environment" + fs + "overworld_cubemap").mkdirs();
      for (int i = 0; i < 10; i++) {
        File source = new File(file + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "blocks" + fs + "destroy_stage_" + i + ".png");
        if (source.exists()) {
          File dest = new File(packPath + fs + "textures" + fs + "environment" + fs + "destroy_stage_" + i + ".png");
          FileUtils.moveFile(source, dest); // was originally copy
        }
      }
    } catch (Exception e) {
      System.out.println("failed to port environment folder");
    }
  }

  private static void font(File file, String packPath) {
    try {
      File source1 = new File(file + fs + "assets" + fs + "minecraft" + fs + "mcpatcher" + fs + "font"); // copy the font
      File source2 = new File(file + fs + "assets" + fs + "minecraft" + fs + "font");
      File source3 = new File(file + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "font");
      File dest = new File(packPath + fs + "font");
      // refactored this greatly to use move instead of copy
      if (source1.exists()) {
        FileUtils.moveDirectory(source1, dest);
      } else if (source2.exists()) {
        FileUtils.moveDirectory(source2, dest);
      } else if (source3.exists()) {
        FileUtils.moveDirectory(source3, dest);
      }
      // now rename the font from ascii to default8, so it works on bedrock
      String path = dest + fs;
      File original = new File(path + "ascii.png");
      File newName = new File(path + "default8.png");
      original.renameTo(newName);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("failed to port font");
    }
  }

  private static void gui(String packPath) {
    try {
      String path = packPath + fs + "textures" + fs + "gui" + fs;
      File original = new File(path + "widgets.png");
      if (original.exists()) {
        File newName = new File(path + "gui.png");
        original.renameTo(newName);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("failed to port gui");
    }
  }

  private static void crossHairFix(String packPath) {
    try {
      String path = packPath + fs + "textures" + fs + "gui" + fs;
      File icons = new File(path + "icons.png");
      if (icons.exists()) {
        BufferedImage readIcons = ImageIO.read(icons);
        // crosshair boxes are base 16 according to icons.png size
        int crossHairSize = readIcons.getWidth() / 16;
        // sub image out the crosshair from the top left of icons.png
        BufferedImage crosshair = readIcons.getSubimage(0, 0, crossHairSize, crossHairSize);
        // create a canvas of the crosshair size
        BufferedImage canvas = new BufferedImage(crossHairSize, crossHairSize, BufferedImage.TYPE_INT_ARGB);
        Graphics g = canvas.getGraphics();
        // fill the canvas with a black rectangle
        g.setColor(Color.black);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        // now draw the crosshair
        g.drawImage(crosshair, 0, 0, null);
        g.dispose();
        ImageIO.write(canvas, "png", new File(packPath + fs + "textures" + fs + "ui" + fs + "cross_hair.png"));
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("failed to port cross hair");
    }
  }

  private static void fire(String packPath) {
    try {
      String path = packPath + fs + "textures" + fs + "blocks" + fs;
      File original = new File(path + "fire_layer_1.png");
      if (original.exists()) {
        File newName = new File(path + "fire_1.png");
        original.renameTo(newName);
      }
      File original2 = new File(path + "fire_layer_0.png");
      if (original2.exists()) {
        File newName2 = new File(path + "fire_0.png");
        original2.renameTo(newName2);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("failed to port fire");
    }
  }

  private static void painting(String packPath) {
    try {
      String path = packPath + fs + "textures" + fs + "painting" + fs;
      File original = new File(path + "paintings_kristoffer_zetterstrand.png");
      if (original.exists()) {
        File newName = new File(path + "kz.png");
        original.renameTo(newName);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("failed to port painting");
    }
  }

  // entire gui container UI port as well (chests and that stuff)
  // a lot of potentially redundant code, could maybe get be improved
  // this part of the process is fairly slow due to having to copy over folders from assets
  private static void containerUI(File file, String packPath, String packName) {
    String packPathFull = packPath + packName;
    File checkInventory = new File(packPathFull + fs + "textures" + fs + "gui" + fs + "container" + fs + "inventory.png");
    File checkChest = new File(packPathFull + fs + "textures" + fs + "entity" + fs + "chest" + fs + "double_normal.png");
    // we only port containers if we know an inventory or a chest in the pack exists
    if (checkInventory.exists() || checkChest.exists()) {
      try {
        String path = packPathFull + fs;
        // First thing to do is to put in the UI folder
        Path destUI = Paths.get(path.replaceAll("\\\\|\\/", Matcher.quoteReplacement(File.separator)));
        File UI = new File(Main.portDir + fs + "assets" + fs + "ui");
        FileUtils.copyDirectoryToDirectory(UI, new File(destUI.toString()));
        // uidx
        Path destUIDX = destUI;
        File UIDX = new File(Main.portDir + fs + "assets" + fs + "uidx");
        FileUtils.copyDirectoryToDirectory(UIDX, new File(destUIDX.toString()));
        // uidx textures folder
        Path texturesUIDXdest = Paths.get(path.replaceAll("\\\\|\\/", Matcher.quoteReplacement(File.separator)) + "textures");
        File texturesUIDX = new File(Main.portDir + fs + "assets" + fs + "textures_uidx" + fs + "uidx");
        FileUtils.copyDirectoryToDirectory(texturesUIDX, new File(texturesUIDXdest.toString()));
        // java assets
        File assets = new File(file + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "gui" + fs + "container");
        File assetsDest = new File(packPathFull + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "gui" + fs + "container" + fs);
        try {
          FileUtils.copyDirectory(assets, assetsDest);
        } catch (Exception ex) {
          // System.out.println("pack being ported does not have a container");
        }
        // recipe book
        File recipeBook = new File(Main.portDir + fs + "assets" + fs + "recipe_book");
        FileUtils.copyDirectoryToDirectory(recipeBook, new File(path + "assets/uidx/textures/gui/container"));
        FileUtils.copyDirectoryToDirectory(recipeBook, new File(texturesUIDXdest.toString()));
        // container
        File containerDest = new File(packPathFull + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "gui" + fs + "container" + fs);
        File container = new File(file + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "gui" + fs + "container");
        try {
          FileUtils.copyDirectory(container, containerDest);
        } catch (Exception ex) {
          // System.out.println("pack being ported does not have a container for assets");
        }
        // chest and end chest
        File generic54 = new File(packPathFull + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "gui" + fs + "container" + fs + "generic_54.png");
        File newGeneric = new File(packPathFull + fs + "assets" + fs + "uidx" + fs + "textures" + fs + "gui" + fs + "container" + fs + "generic_54.png");

        try {
          FileUtils.copyFile(generic54, newGeneric);
          File renameEndChest = new File(packPathFull + fs + "assets" + fs + "uidx" + fs + "textures" + fs + "gui" + fs + "container" + fs + "generic_54.png");
          File ender_chest = new File(packPathFull + fs + "assets" + fs + "uidx" + fs + "textures" + fs + "gui" + fs + "container" + fs + "ender_chest.png");
          renameEndChest.renameTo(ender_chest);
        } catch (Exception e) {
          // System.out.println("generic54.png does not exist");
        }

        try {
          FileUtils.copyFile(generic54, newGeneric);
          File renameChest = new File(packPathFull + fs + "assets" + fs + "uidx" + fs + "textures" + fs + "gui" + fs + "container" + fs + "generic_54.png");
          File small_chest = new File(packPathFull + fs + "assets" + fs + "uidx" + fs + "textures" + fs + "gui" + fs + "container" + fs + "small_chest.png");
          renameChest.renameTo(small_chest);
        } catch (Exception e) {
          // System.out.println("generic54.png does not exist");
        }

        File inventoryCreativeCheck = new File(packPathFull + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "gui" + fs + "container" + fs + "creative_inventory");
        if (!inventoryCreativeCheck.exists()) {
          File creative = new File(Main.portDir + fs + "assets" + fs + "container" + fs + "creative_inventory");
          FileUtils.copyDirectoryToDirectory(creative, inventoryCreativeCheck.getParentFile());
        }

        try {
          // handle this later in check catch if it fails
          FileUtils.copyFile(generic54, newGeneric);
          File renameChest = new File(packPathFull + fs + "assets" + fs + "uidx" + fs + "textures" + fs + "gui" + fs + "container" + fs + "generic_54.png");
          File small_chest = new File(packPathFull + fs + "assets" + fs + "uidx" + fs + "textures" + fs + "gui" + fs + "container" + fs + "small_chest.png");
          renameChest.renameTo(small_chest);
        } catch (Exception e) {
          // System.out.println("can not find generic54, copying over default container assets");
          File containerDir = new File(Main.portDir + fs + "assets" + fs + "container" + fs);
          new File(Main.portDir + fs + packName + fs + "tempIMGS").mkdirs();
          File tempIMGS = new File(Main.portDir + fs + packName + fs + "tempIMGS");
          FileUtils.copyDirectoryToDirectory(containerDir, tempIMGS);
          File temps = new File(tempIMGS + fs + "container");
          File[] imgs = temps.listFiles();
          for (File currentGUI : imgs) {
            File checker = new File(packPathFull + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "gui" + fs + "container" + fs + currentGUI.getName());
            if (!checker.exists() && !currentGUI.isDirectory()) {
              // System.out.println("Importing default container image " + currentGUI);
              try {
                FileUtils.copyFile(currentGUI, checker);
              } catch (Exception ex) {
                // System.out.println("save location does not exist (no container)");
              }
            }
          }
          try {
            // small_chest handling from here
            File uidxContainer = new File(packPathFull + fs + "assets" + fs + "uidx" + fs + "textures" + fs + "gui" + fs + "container");
            File generic = new File(packPathFull + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "gui" + fs + "container" + fs + "generic_54.png");
            File genDest = new File(uidxContainer + fs + "generic_54.png");
            FileUtils.copyFile(generic, genDest);
            File smallChest = new File(uidxContainer + fs + "small_chest.png");
            genDest.renameTo(smallChest);
            FileUtils.copyFile(generic, genDest);
            File eChest = new File(uidxContainer + fs + "ender_chest.png");
            genDest.renameTo(eChest);
            FileUtils.copyFile(generic, genDest);
          } catch (Exception e1) {
            //  System.out.println("failed to port small_chest.png");
          }
        }
      } catch (Exception e) {
        System.out.println("failed to port container");
      }

      // scale method via global_variables.json editing
      try {
        Path global_variables = Paths.get(packPathFull + fs + "ui" + fs + "_global_variables.json");
        Charset charset = StandardCharsets.UTF_8;
        String content = Files.readString(global_variables, charset);
        String[] containerNames = {"inventory", "generic_54", "brewing_stand", "crafting_table", "furnace", "blast_furnace", "smoker", "enchanting_table", "anvil", "hopper", "dispenser", "beacon", "horse"};
        // iterate through the containers and fill in the resolutions
        for (String containerName : containerNames) {
          String currentName = containerName; // this is used for file look up only
          // blast furnace and smoker share the furnace UI
          if (containerName.equals("blast_furnace") || containerName.equals("smoker")) {
            currentName = "furnace";
          }
          File containerFile = new File(packPathFull + fs + "assets" + fs + "minecraft" + fs + "textures" + fs + "gui" + fs + "container" + fs + currentName + ".png"); // file look up
          if (containerFile.exists()) {
            BufferedImage containerImage = ImageIO.read(containerFile);
            int height = containerImage.getHeight();
            int width = containerImage.getWidth();
            // System.out.println(containerName + " | width: " + width + " | height: " + height);
            int dimension = height;
            // Valid height and width values
            int[] validDimensions = {256, 512, 1024, 2048, 4096, 8192};
            // Check if the height and width are valid dimensions
            boolean validHeight = Arrays.stream(validDimensions).anyMatch(x -> x == height);
            boolean validWidth = Arrays.stream(validDimensions).anyMatch(x -> x == width);
            // If height and width are not equal or not valid, rescale to the closest valid width value
            if (height != width || !validHeight || !validWidth) {
              dimension = findClosestDimension(height, width, validDimensions);
              // System.out.println(containerName + " | " + dimension);
              FileImageUtils.resizeImageWrite(containerFile, containerFile, dimension, dimension, "png");
            }
            content = content.replaceAll("\"\\$" + containerName + "_resolution\": \"256x\",", "\"\\$" + containerName + "_resolution\"\\: \"" + dimension + "x\",");
          }
        }
        // once we have updated the content we can write its new data to the file
        Files.writeString(global_variables, content, charset);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static int findClosestDimension(int height, int width, int[] validDimensions) {
    int averageDimension = (height + width) / 2;
    int closestDimension = validDimensions[0];
    int closestDifference = Math.abs(validDimensions[0] - averageDimension);

    for (int dimension : validDimensions) {
      int difference = Math.abs(dimension - averageDimension);
      if (difference < closestDifference) {
        closestDimension = dimension;
        closestDifference = difference;
      }
    }

    return closestDimension;
  }

  // compresses to an mcpack file
  private static void finalizePort(String packPath) {
    try {
      File pack = new File(packPath);
      ZipUtil.LightZip(pack.getAbsolutePath()); // this copies the file to a zip
      File zippedPack = new File(packPath + ".zip");
      File mcpack = new File(packPath + ".mcpack");
      zippedPack.renameTo(mcpack);
      System.out.println(" Successfully Ported Pack: \n " + mcpack);
    } catch (Exception e) {
      System.out.println("failed in final port stage clean up");
    }
  }

}
