package team24.calender.domain.user;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Document(collection = "user")
public class
Userinfo {
    private String uid;
    private String nickName;
    private String email;
    private String image;
    private String loginMethod;
    /*
    private String gender;
    private String age;
    private String route;
    */
    private String startDate;
    private String fcmToken;
}
