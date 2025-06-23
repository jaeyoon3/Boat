package team24.calender.domain.Vote;


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
@Document(collection = "Comment")
public class Comment {
    private String vid;
    private String gid;
    private String uid;
    private String cid;
    private String nickName;
    private String cImage;
    private String comment;
    private int complainCount;
    private LocalDateTime commentDate;


}
