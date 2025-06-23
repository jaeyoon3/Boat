package team24.calender.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component // Spring에서 이 클래스를 Bean으로 인식하도록 @Component 추가
public class VoteWebSocketHandler extends TextWebSocketHandler {

    // WebSocket 세션을 관리하는 Set

    private final Map<String, WebSocketSession> clients = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();



    // WebSocket 연결이 열렸을 때 실행되는 메서드
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Client connected: " + session.getId());
    }
    private Map<String, String> parseJson(String json) throws IOException {
        // Jackson을 사용해 JSON을 Map으로 변환
        return objectMapper.readValue(json, Map.class);
    }
    // 클라이언트가 메시지를 보냈을 때 처리하는 메서드
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Received message: " + payload);

        Map<String, String> data = parseJson(payload);
        String action = data.get("action");

        if ("id".equals(action)) {
            // 클라이언트가 고유 ID를 보내면 해당 ID와 WebSocketSession을 저장
            String clientId = data.get("clientID");
            clients.put(clientId, session);
            System.out.println("Client registered with ID: " + clientId);
        }else if ("stop".equals(action)) {
            String uid = data.get("uid");
            VoteChangeStreamService.handleMessage(action, null, uid);  // stop은 gid가 필요없음
        }else  if ("start".equals(action)) {
            String gid = data.get("gid");
            String uid = data.get("uid");
            // 메시지를 처리하는 로직에서 직접 의존성을 갖지 않고, 해당 서비스에서 처리하도록 전달
            VoteChangeStreamService.handleMessage(action, gid, uid);
        }
        System.out.println("Received message: " + message.getPayload());
    }

    // WebSocket 연결이 닫혔을 때 실행되는 메서드
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String disconnectedClientId = null;
        for (Map.Entry<String, WebSocketSession> entry : clients.entrySet()) {
            if (entry.getValue().equals(session)) {
                disconnectedClientId = entry.getKey();
                break;
            }
        }
        if (disconnectedClientId != null) {
            VoteChangeStreamService.handleMessage("stop", null, disconnectedClientId);  // 연결이 끊기면 ChangeStream 중지
        }
        clients.values().remove(session);
        System.out.println("Client disconnected: " + session.getId());
    }

    // 모든 연결된 세션으로 메시지를 전송하는 메서드
    private void broadcastToAllClients(String message) throws IOException {
        // 모든 클라이언트에게 메시지를 전송
        for (WebSocketSession client : clients.values()) {
            if (client.isOpen()) {
                client.sendMessage(new TextMessage(message));
            }
        }
    }

    void sendMessageToClient(String targetId, String message) throws IOException {
        // 특정 클라이언트에게만 메시지를 전송
        WebSocketSession client = clients.get(targetId);
        if (client != null && client.isOpen()) {
            client.sendMessage(new TextMessage(message));
        }
        System.out.println("Client message " + targetId);
    }
}
