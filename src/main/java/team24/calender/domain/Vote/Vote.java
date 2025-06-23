package team24.calender.domain.Vote;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Document(collection = "Vote")
public class Vote {
    private String gid;
    private String groupName;
    private String vid;
    private String subject;
    private String contents;
    private LocalDateTime date;
    private LocalDateTime endTime;
    private String address;
    private String[] agreeUid;
    private String[] disagreeUid;
    private String[] emoticonUid1;
    private String[] emoticonUid2;
    private String[] emoticonUid3;
    private String[] emoticonUid4;
    private String[] emoticonUid5;
    private int max;
    private String organizer;
    private String organizerNickname;
    private String organizerImage;
    private boolean passed;
    private boolean firstChecked;
    private boolean firstCome;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vote vote = (Vote) o;
        return max == vote.max &&
                passed == vote.passed &&
                firstChecked == vote.firstChecked &&
                firstCome == vote.firstCome &&
                Objects.equals(gid, vote.gid) &&
                Objects.equals(groupName, vote.groupName) &&
                Objects.equals(vid, vote.vid) &&
                Objects.equals(subject, vote.subject) &&
                Objects.equals(contents, vote.contents) &&
                Objects.equals(date, vote.date) &&
                Objects.equals(endTime, vote.endTime) &&
                Objects.equals(address, vote.address) &&
                Arrays.equals(agreeUid, vote.agreeUid) &&
                Arrays.equals(disagreeUid, vote.disagreeUid) &&
                Arrays.equals(emoticonUid1, vote.emoticonUid1) &&
                Arrays.equals(emoticonUid2, vote.emoticonUid2) &&
                Arrays.equals(emoticonUid3, vote.emoticonUid3) &&
                Arrays.equals(emoticonUid4, vote.emoticonUid4) &&
                Arrays.equals(emoticonUid5, vote.emoticonUid5) &&
                Objects.equals(organizer, vote.organizer) &&
                Objects.equals(organizerNickname, vote.organizerNickname) &&
                Objects.equals(organizerImage, vote.organizerImage);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(gid, groupName, vid, subject, contents, date, endTime, address, max, organizer, organizerNickname, organizerImage, passed, firstChecked, firstCome);
        result = 31 * result + Arrays.hashCode(agreeUid);
        result = 31 * result + Arrays.hashCode(disagreeUid);
        result = 31 * result + Arrays.hashCode(emoticonUid1);
        result = 31 * result + Arrays.hashCode(emoticonUid2);
        result = 31 * result + Arrays.hashCode(emoticonUid3);
        result = 31 * result + Arrays.hashCode(emoticonUid4);
        result = 31 * result + Arrays.hashCode(emoticonUid5);
        return result;
    }
}
