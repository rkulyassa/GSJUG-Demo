import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
    public static long getChannelViews(String channelID) {
        String channelData = "";
        try {
            Elements scriptElements = Jsoup.connect("https://www.youtube.com/channel/"+channelID+"/about").get().getElementsByTag("script");
            for (Element script : scriptElements) {
                String data = script.data();
                if (data.startsWith("var ytInitialData")) {
                    channelData = data.substring(20, data.length()-1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String viewsText = channelData.substring(channelData.indexOf("viewCountText")+30, channelData.indexOf("joinedDateText")-10);
        long views = Long.parseLong(viewsText.replace(",",""));
        return views;
    }

    public static void insertDB(MongoCollection<Document> collection, String channelID, long views) {
        Document object = new Document("id", channelID)
        .append("views", String.valueOf(views));
        collection.insertOne(object);
    }

    public static void updateDB(MongoCollection<Document> collection, String channelID, long views) {
        collection.updateOne(Filters.eq("channelID", channelID), Updates.set("views", views));
    }

    public static ArrayList<String> readIDs(String file) {
        ArrayList<String> ids = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                ids.add(line.split(",")[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public static boolean checkIfIDExists(MongoCollection<Document> collection, String channelID) {
        MongoCursor<Document> cursor = collection.find().cursor();
        while (cursor.hasNext()) {
            String checkID = (String) cursor.next().get("id");
            if (checkID.equals(channelID)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb+srv://admin:admin@cluster0.saigp6w.mongodb.net/myFirstDatabase"));
        MongoDatabase database = mongoClient.getDatabase("cluster0");
        MongoCollection<Document> collection = database.getCollection("sampleCollection");

        ArrayList<String> channelIDs = readIDs("ids.csv");

        for (String channelID : channelIDs) {
            if (checkIfIDExists(collection, channelID)) {
                updateDB(collection, channelID, getChannelViews(channelID));
            } else {
                insertDB(collection, channelID, getChannelViews(channelID));
            }
        }
    }
}