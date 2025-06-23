package team24.calender.webSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final VoteWebSocketHandler voteWebSocketHandler;

    public WebSocketConfig(VoteWebSocketHandler voteWebSocketHandler) {
        this.voteWebSocketHandler = voteWebSocketHandler; // 의존성 주입
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // WebSocket 엔드포인트 등록
        registry.addHandler(voteWebSocketHandler, "/vote-websocket").setAllowedOrigins("*");
    }
}
