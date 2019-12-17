//package dao;
//
//import com.mongodb.*;
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoDatabase;
//import com.mongodb.client.model.CreateCollectionOptions;
//import com.mongodb.connection.ServerConnectionState;
//import com.mongodb.event.ServerHeartbeatFailedEvent;
//import com.mongodb.event.ServerHeartbeatStartedEvent;
//import com.mongodb.event.ServerHeartbeatSucceededEvent;
//import com.mongodb.event.ServerMonitorListener;
//import conf.Configuration;
//import org.bson.Document;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class WebAppRepositoryClient implements ServerMonitorListener{
//
//    private static WebAppRepositoryClient instance;
//    static MongoClient client;
//    MongoCollection<Document> testlogCollection;
//    private static Logger logger = LoggerFactory.getLogger(WebAppRepositoryClient.class);
//    private static Configuration conf = Configuration.getInstance();
//
//    private WebAppRepositoryClient() {
//
//        try {
//            client = new MongoClient(
//                    new ServerAddress("localhost", 27018),
//                    MongoCredential.createCredential(conf.getRepositoryUsername(), "admin", conf.getRepositoryPassword().toCharArray()),
//                    MongoClientOptions.builder()
//                            .addServerMonitorListener(this)
//                            .build()
//            );
//
//            MongoDatabase db = client.getDatabase("e2ewd");
//            if (!collectionExists(db)){
//
//                logger.info("Collection 'testlog' doesn't exist. Creating...");
//                db.createCollection("testlog",
//                        new CreateCollectionOptions().capped(true).maxDocuments(8640).sizeInBytes(20000000));
//                logger.info("Collection 'testlog' created in database 'e2ewd'");
//            }
//
//            testlogCollection = db.getCollection("testlog");
//        } catch (MongoSecurityException | MongoCommandException e){
//            logger.warn("Unable to create MongoDB client: " +  e.getMessage());
//        }
//
//
//    }
//
//    public static WebAppRepositoryClient getInstance() {
//
//        if (instance == null) {
//            instance = new WebAppRepositoryClient();
//            logger.debug("Created mongodb client instance");
//        }
//
//        return instance;
//    }
//
//    private boolean collectionExists(MongoDatabase db){
//
//        for (String collection : db.listCollectionNames()){
//                if (collection.equals("testlog")){
//                    return true;
//                }
//            }
//
//        return false;
//
//    }
//
//    public void insertTest(Document testLogDocument){
//
//        testlogCollection.insertOne((testLogDocument));
//        logger.info("Test log inserted into Mongo");
//        closeClient();
//
//    }
//
//    public static void closeClient() {
//        client.close();
//    }
//
//    @Override
//    public void serverHearbeatStarted(ServerHeartbeatStartedEvent event) {
//
//        logger.debug("Connection to MongoDB started...");
//
//    }
//
//    @Override
//    public void serverHeartbeatSucceeded(ServerHeartbeatSucceededEvent event) {
//
//        logger.info("Connection to MongoDB successful.");
//
//    }
//
//    @Override
//    public void serverHeartbeatFailed(ServerHeartbeatFailedEvent event) {
//
//        logger.warn("Failed connecting to MongoDB. Makes sure the credentials are valid.");
//        closeClient();
//
//    }
//}
