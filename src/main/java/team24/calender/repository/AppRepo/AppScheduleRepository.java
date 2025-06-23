package team24.calender.repository.AppRepo;

import org.springframework.data.mongodb.repository.MongoRepository;
import team24.calender.domain.Group.Group;
import team24.calender.domain.Schedule.Schedule;


public interface AppScheduleRepository extends MongoRepository<Schedule, String> {
    Schedule findBySid(String sid);
}
