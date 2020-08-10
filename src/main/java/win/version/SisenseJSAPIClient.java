package version;

import conf.Configuration;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class SisenseJSAPIClient {

    public static String getVersion() throws JSONException, IOException {

        StringBuilder urlSb = new StringBuilder();
        urlSb.append(Configuration.getInstance().getProtocol());
        urlSb.append("://");
        urlSb.append(Configuration.getInstance().getHost());
        if (Configuration.getInstance().getPort() != 443) {
            urlSb.append(":");
            urlSb.append(Configuration.getInstance().getPort());
        }
        urlSb.append("/app/account#/login");

        String url = urlSb.toString();
        Document document =
                Jsoup.connect(url)
                        .timeout(1000)
                        .get();

        Elements scriptElements = document.getElementsByTag("script");

        for (Element element :scriptElements ){

            for (DataNode node : element.dataNodes()) {
                String elementInnerText = node.getWholeData();

                if (elementInnerText.contains("window.prism")){
                    JSONObject prism = new JSONObject(elementInnerText.split("=")[1].trim());
                    return prism.getString("version");
                }

            }
        }

        return null;
    }
}
