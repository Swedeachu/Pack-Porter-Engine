package swim.porter.engine;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import swim.porter.Main;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader {

  final static String fs = File.separator;

  // returns the path of the downloaded pvprp file
  public static File downloadPVPRP(String url) {
    try {
      assert url != null;
      URL checkSite = new URL(url);
      HttpURLConnection huc = (HttpURLConnection) checkSite.openConnection();
      int responseCode = huc.getResponseCode();
      if (url.contains("pvprp.com/pack?p=") && responseCode == 200) {
        Document document = Jsoup.connect(url).get();
        Elements scriptElements = document.getElementsByTag("script");
        String funct = "";
        for (Element element : scriptElements) {
          for (DataNode node : element.dataNodes()) {
            funct = node.getWholeData();
          }
        }
        String[] assetSplit = funct.split("assets/packs");
        assetSplit = assetSplit[1].split("\"");
        String downloadURL = assetSplit[0];
        String downloadURLBase = downloadURL.substring(0, downloadURL.indexOf('?'));
        String formattedURL = new URIBuilder().setPath("pvprp.com/assets/packs" + downloadURLBase).toString().substring(1);
        File portBotDir = new File(Main.portDir); // important
        Elements fileName = document.getElementsByClass("f-mc extra-large shadow");
        String name = "placeHolderName";
        for (Element el : fileName) {
          name = el.text();
        }
        File export = new File(portBotDir + fs + name + ".zip");
        FileUtils.copyURLToFile(new URL("https://" + formattedURL), export, 30000, 30000);
        return export;
      } else {
        System.out.println("Error : Unable to download pack from PVPRP");
      }
    } catch (Exception e) {
      System.out.println("Error downloading pvprp pack, check the pack for errors and remove any illegal characters in file paths!");
      e.printStackTrace();
    }
    return null;
  }

  // returns the path of the downloaded mediafire file
  public static File downloadMediafire(String originalURL, boolean isRar) {
    // we take advantage of corsproxy to bypass mediafire's cloudflare protection
    // String url = "https://corsproxy.io/?" + URLEncoder.encode(originalURL, "UTF-8"); // jsoup 1.15.4 or something else random made encoding not needed
    String url = "https://corsproxy.io/?" + originalURL;
    Document document = null;
    try {
      document = Jsoup.connect(url).get();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Elements buttons = document.getElementsByClass("input popsok"); // get a list of the element class we care about
    if (!buttons.isEmpty()) {
      Element directDownload = document.getElementById("downloadButton"); // can cause an exception at times
      assert directDownload != null;
      String[] hrefSplit = directDownload.outerHtml().split("href=\"");
      String[] href = hrefSplit[1].split("\"");
      try {
        Elements fileName = document.getElementsByClass("dl-btn-label");
        String name = "placeHolderName";
        for (Element el : fileName) {
          name = el.text();
        }
        assert originalURL != null;
        File portBotDir = new File(Main.portDir); // important
        File export = new File(portBotDir + fs + name + ".zip");
        if (isRar) {
          export = new File(portBotDir + fs + name + ".rar");
        }
        FileUtils.copyURLToFile(new URL(href[0]), export, 30000, 30000);
        return export; // return the path of the file we just downloaded
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("mediafire link error");
    }
    return null;
  }

}
