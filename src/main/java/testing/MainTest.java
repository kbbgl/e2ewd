package testing;

import java.net.MalformedURLException;
import java.net.URL;

public class MainTest {

    public static void main(String[] args) {

        String url1 = "";
        String url2 = "https://hooks.slack.com/services/ZZZZZZZZZ/YYYYYYYY/XXXXXXXXXXXXXXXXX";

        try {
            URL check = new URL(url1);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            URL check = new URL(url2);
            System.out.println(check.getHost());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

}
