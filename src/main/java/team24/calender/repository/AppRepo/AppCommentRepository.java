package team24.calender.repository.AppRepo;

import org.springframework.data.mongodb.repository.MongoRepository;
import team24.calender.domain.Vote.Comment;

public interface AppCommentRepository  extends MongoRepository<Comment, String> {
    Comment findCommentByCid(String cid);
}
