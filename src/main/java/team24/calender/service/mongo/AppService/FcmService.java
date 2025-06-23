package team24.calender.service.mongo.AppService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Service;
import team24.calender.domain.Fcm.RequestDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    public void sendPushs(String title, String body, List<String> targetUserTokens) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance().sendEachForMulticast(makeMessages(title, body, targetUserTokens));
        System.out.println("SUCCESS");
    }
    public static MulticastMessage makeMessages(String title, String body, List<String> targetTokens) throws JsonParseException{
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
        return MulticastMessage.builder()
                .setNotification(notification)
                .addAllTokens(targetTokens)
                .build();

    }
}