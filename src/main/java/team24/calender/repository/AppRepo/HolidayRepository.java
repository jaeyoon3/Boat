package team24.calender.repository.AppRepo;

import org.springframework.data.mongodb.repository.MongoRepository;
import team24.calender.domain.Holiday.Holiday;

public interface HolidayRepository extends MongoRepository<Holiday, String> {
    boolean existsByHolidayDate(String holidayDate);
}
