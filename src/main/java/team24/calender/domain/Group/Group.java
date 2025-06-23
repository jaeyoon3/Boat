package team24.calender.domain.Group;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Document(collection = "Group")
public class Group {
    private String gid;
    private String[] uid;
    private String groupName;
    private String groupCategory;
    private String groupNotice;
    private String groupImage;
}
