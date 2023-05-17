package swim.porter.engine;

import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

  // lightweight method for zipping a directory relatively quick
  // not sure if using 7zip binding would have any benefit beyond speed, which would be worth it if that is the case
  public static void LightZip(String dirPath) {
    final Path sourceDir = Paths.get(dirPath);
    String zipFileName = dirPath.concat(".zip");
    try {
      final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
      Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
          try {
            Path targetFile = sourceDir.relativize(file);
            outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
            byte[] bytes = Files.readAllBytes(file);
            outputStream.write(bytes, 0, bytes.length);
            outputStream.closeEntry();
          } catch (IOException e) {
            System.out.println("file compression error");
          }
          return FileVisitResult.CONTINUE;
        }
      });
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // a lightweight unzip method that ignores null data
  public static void lightUnzip(File zipFile, File outputDir) {
    try (ZipFile zf = new ZipFile(zipFile)) {
      Enumeration<ZipArchiveEntry> entries = zf.getEntries();
      while (entries.hasMoreElements()) {
        ZipArchiveEntry entry = entries.nextElement();
        try {
          File outputFile = new File(outputDir, entry.getName());
          if (entry.isDirectory()) {
            Files.createDirectories(Paths.get(outputFile.getAbsolutePath()));
          } else {
            Files.createDirectories(Paths.get(outputFile.getParent()));
            try (InputStream inputStream = zf.getInputStream(entry); FileOutputStream outputStream = new FileOutputStream(outputFile)) {
              byte[] buffer = new byte[1024];
              int bytesRead;
              while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
              }
            }
          }
        } catch (IOException e) {
          System.err.println("Error processing entry: " + entry.getName() + ". Skipping it.");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // we use 7zip binding to extract any type of archive possible, but using lightUnzip is a lot faster for normal zip archives
  public static void SevenExtract(String archiveFilePath, String outputDirectory) {
    try {
      RandomAccessFile randomAccessFile = new RandomAccessFile(archiveFilePath, "r");
      RandomAccessFileInStream randomAccessFileStream = new RandomAccessFileInStream(randomAccessFile);
      IInArchive inArchive = SevenZip.openInArchive(null, randomAccessFileStream);
      try {
        for (ISimpleInArchiveItem item : inArchive.getSimpleInterface().getArchiveItems()) {
          if (!item.isFolder()) {
            try {
              File outputFile = new File(outputDirectory, item.getPath());
              if (!outputFile.isDirectory()) {
                Files.createDirectories(Paths.get(outputFile.getParent()));
                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                  ExtractOperationResult result = item.extractSlow(data -> {
                    if (data == null) {
                      return 0;
                    }
                    try {
                      outputStream.write(data);
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                    return data.length;
                  });
                  if (result != ExtractOperationResult.OK) {
                    System.err.println(String.format("Error extracting archive item %s. Extracting error: %s", item.getPath(), result));
                  }
                }
              }
            } catch (Exception e) {
              System.err.println("Error processing item: " + item.getPath() + ". Skipping it.");
              e.printStackTrace();
            }
          }
        }
      } finally {
        inArchive.close();
        randomAccessFileStream.close();
        randomAccessFile.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
