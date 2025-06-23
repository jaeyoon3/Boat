package team24.calender.domain.UserGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Document(collection = "UserGroup")
public class UserGroup {
    private String uid;
    private String gid;
    private String groupNickname;
    private String groupRoll;
    private String groupPersonalEmail;
    private String groupPersonalPhone;
    private String groupPersonalAddress;
    private String imageUrl;
}
