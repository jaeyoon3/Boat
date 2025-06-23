package team24.calender.repository.AppRepo;

import org.springframework.data.mongodb.repository.MongoRepository;
import team24.calender.domain.user.Userinfo;

public interface AppUserRepository extends MongoRepository<Userinfo, String> {
    Userinfo findByUid(String uid);
}
