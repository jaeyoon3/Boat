package team24.calender.domain.Schedule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Document(collection = "Schedule")
public class Schedule {
    private String uid;
    private String sid;
    private String scheduleSubject;
    private String scheduleContents;
    private String scheduleAddress;
    private String color;
    private boolean checked;
    private LocalDateTime scheduleEndDate;
    private LocalDateTime scheduleStartDate;
}
