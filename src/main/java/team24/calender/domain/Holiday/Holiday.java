package team24.calender.domain.Holiday;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Document(collection = "Holiday")
public class Holiday {
    private LocalDate holidayDate;
    private String holidayName;
}
