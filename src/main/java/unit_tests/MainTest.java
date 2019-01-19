package unit_tests;

import integrations.SlackClient;

public class MainTest {

    public static void main(String[] args) {

        SlackClient slackClient = SlackClient.getInstance();
        slackClient.sendMessage();

    }

}
