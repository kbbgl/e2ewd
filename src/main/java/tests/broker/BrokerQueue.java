package tests.broker;

public class BrokerQueue {

    public int numberOfConsumers;    // consumers
    public int messageBytes;         // message_bytes
    public int numberOfMessages;     // messages
    public int processMemoryInBytes; // memory
    public String name;              // name

    public BrokerQueue(int numberOfConsumers, int messageBytes, int numberOfMessages, int processMemoryInBytes, String name) {
        this.numberOfConsumers = numberOfConsumers;
        this.messageBytes = messageBytes;
        this.numberOfMessages = numberOfMessages;
        this.processMemoryInBytes = processMemoryInBytes;
        this.name = name;
    }

    public int getNumberOfConsumers() {
        return numberOfConsumers;
    }

    public int getMessageBytes() {
        return messageBytes;
    }

    @Override
    public String toString() {
        return "BrokerQueue{\n" +
                "\tnumberOfConsumers=" + numberOfConsumers + ",\n" +
                "\tmessageBytes=" + messageBytes + ",\n" +
                "\tnumberOfMessages=" + numberOfMessages + ",\n" +
                "\tprocessMemoryInBytes=" + processMemoryInBytes + ",\n" +
                "\tname='" + name + "'\n" +
                '}';
    }

    public int getNumberOfMessages() {
        return numberOfMessages;
    }

    public int getProcessMemoryInBytes() {
        return processMemoryInBytes;
    }

    public String getName() {
        return name;
    }
}
