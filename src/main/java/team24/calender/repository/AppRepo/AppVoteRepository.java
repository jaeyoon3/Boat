package team24.calender.repository.AppRepo;

import org.springframework.data.mongodb.repository.MongoRepository;
import team24.calender.domain.Vote.Vote;


public interface AppVoteRepository extends MongoRepository<Vote, String> {
    Vote findByVid(String vid);
}
