package team24.calender.service.mongo.AppService;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import team24.calender.domain.Vote.Vote;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class LongPolling {
    private final MongoTemplate mongoTemplate;
    private final AppService appService;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public void checkVote(String vid,String organizer,String subject, DeferredResult<Boolean> deferredResult) {
        scheduler.scheduleAtFixedRate(() -> {
            Query query = new Query(Criteria.where("vid").is(vid));
            Vote vote = mongoTemplate.findOne(query, Vote.class);
            if (vote != null) {
                int max = vote.getMax();
                boolean passed = vote.getAgreeUid().length >= max;
                if (passed) {
                    Update update = new Update().set("passed", passed);
                    mongoTemplate.updateMulti(query, update, Vote.class);
                    deferredResult.setResult(true);
                }
                else{
                    try {
                        appService.sendEndMessage(vid, organizer, subject);
                    } catch (FirebaseMessagingException e) {
                        throw new RuntimeException(e);
                    }
                    mongoTemplate.remove(query, Vote.class);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
