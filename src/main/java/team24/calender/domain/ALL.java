package team24.calender.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import team24.calender.domain.Group.Group;
import team24.calender.domain.Schedule.Schedule;
import team24.calender.domain.UserGroup.UserGroup;
import team24.calender.domain.Vote.Vote;


import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class ALL {
    private List<Group> groups;
    private List<Schedule> schedules;
    private List<UserGroup> userGroups;
    private List<Vote> votes;
}
