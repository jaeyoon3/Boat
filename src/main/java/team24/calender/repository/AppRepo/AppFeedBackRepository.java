package team24.calender.repository.AppRepo;

import org.springframework.data.mongodb.repository.MongoRepository;
import team24.calender.domain.FeedBack.FeedBack;


public interface AppFeedBackRepository extends MongoRepository<FeedBack, String> {
    FeedBack findFeedBackByUid(String uid);
}
