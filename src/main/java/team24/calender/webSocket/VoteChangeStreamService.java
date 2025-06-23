package team24.calender.webSocket;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.model.changestream.FullDocument;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.Document;
import team24.calender.domain.Vote.Vote;
import team24.calender.service.mongo.AppService.AppService;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;


@Service
public class VoteChangeStreamService {

    private final VoteWebSocketHandler voteWebSocketHandler;
    private final AppService appService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    public VoteChangeStreamService(VoteWebSocketHandler voteWebSocketHandler, AppService appService) {
        this.voteWebSocketHandler = voteWebSocketHandler; // 의존성 주입
        this.appService = appService;
    }
    String uri = "uri";

    private final Map<String, Future<?>> activeChangeStreams = new ConcurrentHashMap<>();

    ServerApi serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build();
    MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(uri))
            .serverApi(serverApi)
            .build();
    // 특정 gid로 문서를 처음에 모두 전송

    public static void handleMessage(String action, String gid, String uid) {
        VoteChangeStreamService service = ApplicationContextProvider.getBean(VoteChangeStreamService.class);
        if ("start".equals(action)) {
            service.startWatchingForGid(gid, uid);
        } else if ("stop".equals(action)) {
            service.stopWatchingForUid(uid);
        }
    }
    public void sendAllVotesByGid(String gid,String uid) {
        try (MongoClient mongoClient = MongoClients.create(settings)) {
            MongoDatabase database = mongoClient.getDatabase("team24"); // DB 이름
            MongoCollection<Document> collection = database.getCollection("Vote");  // 컬렉션 이름
            System.out.println("조회 start"+collection);
            JSONArray jsonArray = new JSONArray();
            // 해당 gid로 모든 문서 조회
            List<Document> voteDocuments = collection.find(new Document("gid", gid)).into(new ArrayList<>());
            for (Document doc : voteDocuments) {
                jsonArray.put(new org.json.JSONObject(doc.toJson()));  // 각 Document를 JSONObject로 변환하여 추가
            }
            // 모든 문서를 WebSocket으로 전송
            voteWebSocketHandler.sendMessageToClient(uid,jsonArray.toString());
            System.out.println("doc"+jsonArray);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 해당 gid로 변경 사항을 실시간으로 감지
    public void watchChangeStreamForGid(String gid,String uid) {
        stopWatchingForUid(uid);
        Future<?> future = executorService.submit(() -> {
            try (MongoClient mongoClient = MongoClients.create(settings)) {
                System.out.println("변화감지 초중반"+uid);
                MongoDatabase database = mongoClient.getDatabase("team24"); // DB 이름
                MongoCollection<Document> collection = database.getCollection("Vote");  // 컬렉션 이름
                System.out.println("변화감지 시작");
                List<Bson> pipeline = Collections.singletonList(
                        Aggregates.match(Filters.eq("fullDocument.gid", gid))  // 특정 gid로 필터링
                );
                // ChangeStream을 사용해 votes 컬렉션의 변화를 실시간으로 감지
                collection.watch(pipeline).fullDocument(FullDocument.UPDATE_LOOKUP).forEach((ChangeStreamDocument<Document> changeStreamDocument) -> {
                    System.out.println("변화감지 초중반");
                    JSONArray jsonArray = new JSONArray();
                    List<Document> voteDocuments = collection.find(new Document("gid", gid)).into(new ArrayList<>());
                    for (Document doc : voteDocuments) {
                        try {
                            jsonArray.put(new org.json.JSONObject(doc.toJson()));  // 각 Document를 JSONObject로 변환하여 추가
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        System.out.println("변화감지 초중반2");
                        voteWebSocketHandler.sendMessageToClient(uid,jsonArray.toString());
                        System.out.println("liost2"+jsonArray+"\n"+uid);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        });
        activeChangeStreams.put(uid, future);
    }
    public void stopWatchingForUid(String uid) {
        Future<?> future = activeChangeStreams.get(uid);
        if (future != null && !future.isDone()) {
            // 현재 실행 중인 작업 중지 요청
            future.cancel(true);
            activeChangeStreams.remove(uid);
            System.out.println("ChangeStream 중지됨 - uid: " + uid);
        }
    }
    // gid로 해당 문서들을 먼저 보내고 실시간 감지 시작
    public void startWatchingForGid(String gid,String uid) {
        System.out.println("start");
        // 처음에 해당 gid로 모든 문서 전송
        sendAllVotesByGid(gid,uid);

        // 실시간 변경 감지 시작
        watchChangeStreamForGid(gid,uid);
    }
}

