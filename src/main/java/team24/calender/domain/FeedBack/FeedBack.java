package team24.calender.domain.FeedBack;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Document(collection = "FeedBack")
public class FeedBack {
    private String uid;
    private String feedBack;
}
