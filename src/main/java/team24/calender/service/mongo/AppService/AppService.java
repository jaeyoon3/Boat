package team24.calender.service.mongo.AppService;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import team24.calender.domain.FeedBack.FeedBack;
import team24.calender.domain.Group.Group;
import team24.calender.domain.Schedule.Schedule;
import team24.calender.domain.UserGroup.UserGroup;
import team24.calender.domain.Vote.Comment;
import team24.calender.domain.Vote.Vote;
import team24.calender.domain.user.Userinfo;
import team24.calender.repository.AppRepo.*;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
public class AppService {

    private final MongoTemplate mongoTemplate;
    private final FcmService fcmService;
    @Autowired
    AppCommentRepository appCommentRepository;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    AppVoteRepository appVoteRepository;
    @Autowired
    AppScheduleRepository appScheduleRepository;
    @Autowired
    AppGroupRepository appGroupRepository;
    @Autowired
    AppFeedBackRepository appFeedBackRepository;

    /*user추가*/
    public void AppUserInsert(Userinfo userinfo){
        String uid= userinfo.getUid();
        Userinfo userinfo1=appUserRepository.findByUid(uid);
        if (userinfo1 == null) {
            mongoTemplate.insert(userinfo);
        }
    }

    public Map<String, String> response(String uuid) {
        Userinfo userinfo = appUserRepository.findByUid(uuid);
        Map<String, String> userInfoMap = new HashMap<>();

        if (userinfo != null) {
            userInfoMap.put("uid", userinfo.getUid());
            userInfoMap.put("email", userinfo.getEmail());
            userInfoMap.put("nickName", userinfo.getNickName());
            userInfoMap.put("startDate", userinfo.getStartDate());
            userInfoMap.put("image", userinfo.getImage());
            userInfoMap.put("loginMethod", userinfo.getLoginMethod());
        }

        return userInfoMap;
    }
    public List<Userinfo> UserinfoByUid(String uid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("uid").in(uid));
        return mongoTemplate.find(query, Userinfo.class);
    }
    public void UserRemove(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));

        List<Group> groups = GroupfindByUid(uid);
        List<String> gidList = new ArrayList<>();
        for (Group group : groups) {
            gidList.add(group.getGid());
        }
        for (String gid : gidList) {
            Query query2 = new Query(Criteria.where("gid").is(gid));
            Group existingDocument = mongoTemplate.findOne(query2, Group.class);
            String[] existingMembers = existingDocument.getUid();

            if (existingMembers != null && existingMembers.length > 0) {
                List<String> updatedMembersList = new ArrayList<>(Arrays.asList(existingMembers));
                updatedMembersList.remove(uid);
                String[] updatedMembers = updatedMembersList.toArray(new String[0]);

                Update update = new Update().set("uid", updatedMembers);
                mongoTemplate.updateFirst(query2, update, Group.class);
            }
        }
        mongoTemplate.remove(query, UserGroup.class);
        mongoTemplate.remove(query, Userinfo.class);
    }
    public void UserImageUpdate(String uid,String image) {
        Query query = new Query(Criteria.where("uid").is(uid));
        Update update = new Update().set("image", image);
        mongoTemplate.updateMulti(query, update, Userinfo.class);
    }
    public void UserNicknameUpdate(String uid,String nickname) {
        Query query = new Query(Criteria.where("uid").is(uid));
        Update update = new Update().set("nickName", nickname);
        mongoTemplate.updateMulti(query, update, Userinfo.class);
    }
    public void UserTokenUpdate(String uid,String fcmToken) {
        Query query = new Query(Criteria.where("uid").is(uid));
        Update update = new Update().set("fcmToken",fcmToken);
        mongoTemplate.updateMulti(query, update, Userinfo.class);


    }
    public List<Group> GroupfindByUid(String uid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("uid").in(uid));
        return mongoTemplate.find(query, Group.class);
    }
    public List<UserGroup> UserGroupfindByUid(String uid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("uid").in(uid));
        return mongoTemplate.find(query, UserGroup.class);
    }
    public List<Schedule> Schedulefind(String uid, LocalDate scheduleStartDate) {

        LocalDateTime startOfDay = scheduleStartDate.atStartOfDay();
        LocalDateTime endOfDay = scheduleStartDate.atTime(23, 59, 59);
        // Create the criteria for both name and date range
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("uid").is(uid),
                Criteria.where("scheduleStartDate").gte(startOfDay).lte(endOfDay)
        );
        // Create the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria)
        );
        // Execute the aggregation
        AggregationResults<Schedule> results = mongoTemplate.aggregate(aggregation, "Schedule", Schedule.class);
        return results.getMappedResults();
    }
    public List<Schedule> SchedulefindByUid(String uid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("uid").in(uid));
        return mongoTemplate.find(query, Schedule.class);
    }
    /*스케쥴추가*/
    public void saveSchedule(Schedule schedule, String newSid) {
        schedule.setSid(newSid);
        appScheduleRepository.save(schedule);// 새로운 gid 값
    }
    public String CreateSid(){
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 5;
        Random random = new Random();

        String newSid = random.ints(leftLimit,rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return newSid;
    }
    public List<Schedule> ScheduleFindBySid(String sid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("sid").in(sid));
        return mongoTemplate.find(query, Schedule.class);
    }
    /*스케쥴 업데이트*/
    public void ScheduleUpdate(String uid, String sid, String scheduleSubject, String scheduleContents, String scheduleAddress, LocalDateTime scheduleStartDate,LocalDateTime scheduleEndDate, String color) {
        Query query = new Query(Criteria.where("uid").is(uid).and("sid").is(sid));
        Update update = new Update().set("scheduleSubject", scheduleSubject);
        Update update1 = new Update().set("scheduleContents", scheduleContents);
        Update update2 = new Update().set("scheduleAddress", scheduleAddress);
        Update update3 = new Update().set("scheduleStartDate", scheduleStartDate);
        Update update4 = new Update().set("scheduleEndDate", scheduleEndDate);
        Update update5 = new Update().set("color", color);
        mongoTemplate.updateMulti(query, update, Schedule.class);
        mongoTemplate.updateMulti(query, update1, Schedule.class);
        mongoTemplate.updateMulti(query, update2, Schedule.class);
        mongoTemplate.updateMulti(query, update3, Schedule.class);
        mongoTemplate.updateMulti(query, update4, Schedule.class);
        mongoTemplate.updateMulti(query, update5, Schedule.class);
    }
    /*스케쥴 제거*/
    public void ScheduleRemove(String sid) {
        Query query = new Query(Criteria.where("sid").is(sid));
        mongoTemplate.remove(query, Schedule.class);
        System.out.println(sid);
    }
    public void ScheduleCheck(String sid){
        Query query = new Query(Criteria.where("sid").is(sid));
        Schedule schedule= mongoTemplate.findOne(query,Schedule.class);
        boolean check= schedule.isChecked();
        Update update;
        if(check) {
            update = new Update().set("checked", false);
        }
        else {
            update = new Update().set("checked", true);
        }
        mongoTemplate.updateMulti(query, update, Schedule.class);
    }
    public void sendScheduleMessage(String sid) throws FirebaseMessagingException {
        Query query = new Query(Criteria.where("sid").is(sid));
        Schedule schedule= mongoTemplate.findOne(query,Schedule.class);
        String subject=schedule.getScheduleSubject();
        String uid= schedule.getUid();
        Userinfo userinfo=appUserRepository.findByUid(uid);

        List<String> fcmtoken=new ArrayList<>();
        String token=userinfo.getFcmToken();
        if (token != null && !token.isEmpty()) {
            fcmtoken.add(token);
        }
        System.out.println(userinfo.getFcmToken());
        String title="30분 후에 일정이 있습니다";
        String body=subject;
        fcmService.sendPushs(title,body,fcmtoken);
    }
    public List<Vote> VotefindByGid(String gid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("gid").in(gid));
        return mongoTemplate.find(query, Vote.class);
    }


    public Vote OneVotefindByVid(String vid) {

        return appVoteRepository.findByVid(vid);
    }
    /*VOTE생성 */
    public void saveVote(Vote vote, String newvid,String organizerNickname, String organizerImage, String groupName) {
        vote.setOrganizerImage(organizerImage);
        vote.setOrganizerNickname(organizerNickname);
        vote.setVid(newvid);
        vote.setGroupName(groupName);
        appVoteRepository.save(vote);
    }

    public void VoteRemove(String vid) {
        Query query = new Query(Criteria.where("vid").is(vid));
        mongoTemplate.remove(query, Comment.class);
        mongoTemplate.remove(query, Vote.class);
        System.out.println(vid);
    }
    public String CreateVid(){
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 5;
        Random random = new Random();

        String newVid = random.ints(leftLimit,rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return newVid;
    }
    public List<Vote> VoteFindByVid(String vid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("vid").in(vid));
        return mongoTemplate.find(query, Vote.class);
    }

    /*투표찬성 추가 */
    public void AgreeVoteMember(String vid, String newUid) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);
        System.out.println(vid);
        System.out.println(newUid);
        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getAgreeUid();
            String[] existingDisAgreeMembers=existingDocument.getDisagreeUid();
            int existingMemberSize = existingMembers != null ? existingMembers.length : 0;
            boolean containsUid = false;
            if (existingMembers != null) {
                for (String memberUid : existingMembers) {
                    if (memberUid.equals(newUid)) {
                        containsUid = true;
                        AgreeDeleteMember(vid,newUid);
                        break;
                    }
                }
            }
            if (existingDisAgreeMembers != null) {
                for (String memberDisagreeUid : existingDisAgreeMembers) {
                    if (memberDisagreeUid.equals(newUid)) {
                        disagreeDeleteMember(vid,newUid);
                    }
                }
            }

            // 새로운 uid가 member에 포함되어 있지 않으면 추가
            if (!containsUid) {
                String[] newMembers = new String[existingMemberSize + 1];
                System.arraycopy(existingMembers, 0, newMembers, 0, existingMemberSize);
                newMembers[existingMemberSize] = newUid;

                Update update = new Update()
                        .set("agreeUid", newMembers);
                mongoTemplate.updateMulti(query, update, Vote.class);
            }
            else{
                System.out.println("error");
            }
        }
    }
    /*투표찬성 삭제 */
    public void AgreeDeleteMember(String vid, String uidToRemove) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);

        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getAgreeUid();

            if (existingMembers != null && existingMembers.length > 0) {
                List<String> updatedMembersList = new ArrayList<>(Arrays.asList(existingMembers));
                updatedMembersList.remove(uidToRemove);
                String[] updatedMembers = updatedMembersList.toArray(new String[0]);

                Update update = new Update().set("agreeUid", updatedMembers);
                mongoTemplate.updateMulti(query, update, Vote.class);
            }
        }
    }

    /*투표반대 추가 */
    public void disagreeVoteMember(String vid, String newUid) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);
        System.out.println(vid);
        System.out.println(newUid);
        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getDisagreeUid();
            String[] existingAgreeMembers= existingDocument.getAgreeUid();
            int existingMemberSize = existingMembers != null ? existingMembers.length : 0;
            boolean containsUid = false;
            if (existingMembers != null) {
                for (String memberUid : existingMembers) {
                    if (memberUid.equals(newUid)) {
                        containsUid = true;
                        disagreeDeleteMember(vid,newUid);
                        break;
                    }
                }
            }

            if (existingAgreeMembers != null) {
                for (String memberAgreeUid : existingAgreeMembers) {
                    if (memberAgreeUid.equals(newUid)) {
                        AgreeDeleteMember(vid,newUid);
                    }
                }
            }

            // 새로운 uid가 member에 포함되어 있지 않으면 추가
            if (!containsUid) {
                String[] newMembers = new String[existingMemberSize + 1];
                System.arraycopy(existingMembers, 0, newMembers, 0, existingMemberSize);
                newMembers[existingMemberSize] = newUid;

                Update update = new Update()
                        .set("disagreeUid", newMembers);
                mongoTemplate.updateMulti(query, update, Vote.class);
            }
            else{
                System.out.println("error");
            }
        }
    }
    /*투표반대 삭제 */
    public void disagreeDeleteMember(String vid, String uidToRemove) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);

        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getDisagreeUid();

            if (existingMembers != null && existingMembers.length > 0) {
                List<String> updatedMembersList = new ArrayList<>(Arrays.asList(existingMembers));
                updatedMembersList.remove(uidToRemove);
                String[] updatedMembers = updatedMembersList.toArray(new String[0]);

                Update update = new Update().set("disagreeUid", updatedMembers);
                mongoTemplate.updateMulti(query, update, Vote.class);
            }
        }
    }
    public void MaxAgree(String vid) throws FirebaseMessagingException {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote Document = mongoTemplate.findOne(query, Vote.class);
        String organizer=Document.getOrganizer();
        String subject=Document.getSubject();
        int max= Document.getMax();
        boolean passed = Document.getAgreeUid().length >= max;
        if (passed) {
            Update update = new Update().set("passed", passed);
            mongoTemplate.updateMulti(query, update, Vote.class);
            sendEndMessage(vid,organizer,subject);
        }
    }
    public void checkAgree(String vid,String organizer, String subject) throws FirebaseMessagingException {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote Document = mongoTemplate.findOne(query, Vote.class);
        String gid=Document.getGid();
        boolean firstChecked= false;
        int max= Document.getMax();
        if (Document.getAgreeUid().length >= max && !Document.isFirstChecked()) {
            firstChecked=true;
            Update update = new Update().set("firstChecked", firstChecked);
            mongoTemplate.updateMulti(query, update, Vote.class); // 첫 번째로 조건이 충족되었음을 표시하기 위해 isFirstPassedChecked 값을 true로 설정하고 저장
            sendFirstPassedMessage(gid, organizer, subject);
        }
    }


    /*투표시작시 메시지*/
    public void sendStartMessage(String gid, String organizer,String subject) throws FirebaseMessagingException {
        Query query2 = new Query(Criteria.where("uid").is(organizer).and("gid").is(gid));
        UserGroup userGroup=mongoTemplate.findOne(query2, UserGroup.class);
        String Nickname=userGroup.getGroupNickname();
        Group groups=appGroupRepository.findByGid(gid);
        List<Userinfo> users=new ArrayList<>();
        String [] uid=groups.getUid();
        for (String id : uid) {
            List<Userinfo> userList = UserinfoByUid(id);
            users.addAll(userList);
        }
        List<String> fcmtoken=new ArrayList<>();
        for (Userinfo userinfo : users) {
            String token1= userinfo.getFcmToken();
            if (token1 != null && !token1.isEmpty()) {
                fcmtoken.add(token1);
            }
            System.out.println(userinfo.getFcmToken());
        }
        String title=subject;
        String body=Nickname+"님이 투표를 시작했습니다";
        fcmService.sendPushs(title,body,fcmtoken);
    }
    /*투표 인원이 처음 과반수를 넘었을시 메시지*/
    public void sendFirstPassedMessage(String gid, String organizer,String subject) throws FirebaseMessagingException {
        Query query2 = new Query(Criteria.where("uid").is(organizer).and("gid").is(gid));
        UserGroup userGroup=mongoTemplate.findOne(query2, UserGroup.class);
        String Nickname=userGroup.getGroupNickname();
        Group groups=appGroupRepository.findByGid(gid);
        List<Userinfo> users=new ArrayList<>();
        String [] uid=groups.getUid();
        for (String id : uid) {
            List<Userinfo> userList = UserinfoByUid(id);
            users.addAll(userList);
        }
        List<String> fcmtoken=new ArrayList<>();
        for (Userinfo userinfo : users) {
            String token1= userinfo.getFcmToken();
            if (token1 != null && !token1.isEmpty()) {
                fcmtoken.add(token1);
            }
            System.out.println(userinfo.getFcmToken());
        }
        String title=subject;
        String body=Nickname+"님의 투표가 과반수가 넘었습니다";
        fcmService.sendPushs(title,body,fcmtoken);
    }
    /*투표종료시 메시지*/
    public void sendEndMessage(String vid, String organizer,String subject) throws FirebaseMessagingException {
        Vote vote=OneVotefindByVid(vid);
        String gid=vote.getGid();
        Query query2 = new Query(Criteria.where("uid").is(organizer).and("gid").is(gid));
        UserGroup userGroup=mongoTemplate.findOne(query2, UserGroup.class);
        String Nickname=userGroup.getGroupNickname();
        Group groups=appGroupRepository.findByGid(gid);
        List<Userinfo> users=new ArrayList<>();
        String [] uid=groups.getUid();
        for (String id : uid) {
            List<Userinfo> userList = UserinfoByUid(id);
            users.addAll(userList);
        }
        List<String> fcmtoken=new ArrayList<>();
        for (Userinfo userinfo : users) {
            String token1= userinfo.getFcmToken();
            if (token1 != null && !token1.isEmpty()) {
                fcmtoken.add(token1);
            }
            System.out.println(userinfo.getFcmToken());
        }
        String title=subject;
        String body=Nickname+"님의 투표가 종료되었습니다";
        fcmService.sendPushs(title,body,fcmtoken);
    }
    /*@@@@@@@@@@@@@@@@@@@@@@@@@@@@@여기서 부터 이모티콘 배열 추가 삭제@@@@@@@@@@@@@@@@@@@@@@@@@@@@@*/
    public void emotion1Member(String vid, String newUid) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);
        System.out.println(vid);
        System.out.println(newUid);
        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getEmoticonUid1();
            int existingMemberSize = existingMembers != null ? existingMembers.length : 0;
            boolean containsUid = false;
            if (existingMembers != null) {
                for (String memberUid : existingMembers) {
                    if (memberUid.equals(newUid)) {
                        containsUid = true;
                        emotionDeleteMember1(vid, newUid);
                        break;
                    }
                }
            }
            // 새로운 uid가 member에 포함되어 있지 않으면 추가
            if (!containsUid) {
                String[] newMembers = new String[existingMemberSize + 1];
                System.arraycopy(existingMembers, 0, newMembers, 0, existingMemberSize);
                newMembers[existingMemberSize] = newUid;

                Update update = new Update()
                        .set("emoticonUid1", newMembers);
                mongoTemplate.updateFirst(query, update, Vote.class);
            }

        }
    }
    /*이모1 삭제 */
    public void emotionDeleteMember1(String vid, String uidToRemove) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);

        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getEmoticonUid1();

            if (existingMembers != null && existingMembers.length > 0) {
                List<String> updatedMembersList = new ArrayList<>(Arrays.asList(existingMembers));
                updatedMembersList.remove(uidToRemove);
                String[] updatedMembers = updatedMembersList.toArray(new String[0]);

                Update update = new Update().set("emoticonUid1", updatedMembers);
                mongoTemplate.updateFirst(query, update, Vote.class);
            }
        }
    }
    public void emotion2Member(String vid, String newUid) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);
        System.out.println(vid);
        System.out.println(newUid);
        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getEmoticonUid2();
            int existingMemberSize = existingMembers != null ? existingMembers.length : 0;
            boolean containsUid = false;
            if (existingMembers != null) {
                for (String memberUid : existingMembers) {
                    if (memberUid.equals(newUid)) {
                        containsUid = true;
                        emotionDeleteMember2(vid, newUid);
                        break;
                    }
                }
            }
            // 새로운 uid가 member에 포함되어 있지 않으면 추가
            if (!containsUid) {
                String[] newMembers = new String[existingMemberSize + 1];
                System.arraycopy(existingMembers, 0, newMembers, 0, existingMemberSize);
                newMembers[existingMemberSize] = newUid;

                Update update = new Update()
                        .set("emoticonUid2", newMembers);
                mongoTemplate.updateFirst(query, update, Vote.class);
            }

        }
    }
    /*이모2 삭제 */
    public void emotionDeleteMember2(String vid, String uidToRemove) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);

        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getEmoticonUid2();

            if (existingMembers != null && existingMembers.length > 0) {
                List<String> updatedMembersList = new ArrayList<>(Arrays.asList(existingMembers));
                updatedMembersList.remove(uidToRemove);
                String[] updatedMembers = updatedMembersList.toArray(new String[0]);

                Update update = new Update().set("emoticonUid2", updatedMembers);
                mongoTemplate.updateFirst(query, update, Vote.class);
            }
        }
    }
    public void emotion3Member(String vid, String newUid) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);
        System.out.println(vid);
        System.out.println(newUid);
        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getEmoticonUid3();
            int existingMemberSize = existingMembers != null ? existingMembers.length : 0;
            boolean containsUid = false;
            if (existingMembers != null) {
                for (String memberUid : existingMembers) {
                    if (memberUid.equals(newUid)) {
                        containsUid = true;
                        emotionDeleteMember3(vid, newUid);
                        break;
                    }
                }
            }
            // 새로운 uid가 member에 포함되어 있지 않으면 추가
            if (!containsUid) {
                String[] newMembers = new String[existingMemberSize + 1];
                System.arraycopy(existingMembers, 0, newMembers, 0, existingMemberSize);
                newMembers[existingMemberSize] = newUid;

                Update update = new Update()
                        .set("emoticonUid3", newMembers);
                mongoTemplate.updateFirst(query, update, Vote.class);
            }

        }
    }
    /*이모2 삭제 */
    public void emotionDeleteMember3(String vid, String uidToRemove) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);

        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getEmoticonUid3();

            if (existingMembers != null && existingMembers.length > 0) {
                List<String> updatedMembersList = new ArrayList<>(Arrays.asList(existingMembers));
                updatedMembersList.remove(uidToRemove);
                String[] updatedMembers = updatedMembersList.toArray(new String[0]);

                Update update = new Update().set("emoticonUid3", updatedMembers);
                mongoTemplate.updateFirst(query, update, Vote.class);
            }
        }
    }
    public void emotion4Member(String vid, String newUid) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);
        System.out.println(vid);
        System.out.println(newUid);
        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getEmoticonUid4();
            int existingMemberSize = existingMembers != null ? existingMembers.length : 0;
            boolean containsUid = false;
            if (existingMembers != null) {
                for (String memberUid : existingMembers) {
                    if (memberUid.equals(newUid)) {
                        containsUid = true;
                        emotionDeleteMember4(vid, newUid);
                        break;
                    }
                }
            }
            // 새로운 uid가 member에 포함되어 있지 않으면 추가
            if (!containsUid) {
                String[] newMembers = new String[existingMemberSize + 1];
                System.arraycopy(existingMembers, 0, newMembers, 0, existingMemberSize);
                newMembers[existingMemberSize] = newUid;

                Update update = new Update()
                        .set("emoticonUid4", newMembers);
                mongoTemplate.updateFirst(query, update, Vote.class);
            }

        }
    }
    /*이모2 삭제 */
    public void emotionDeleteMember4(String vid, String uidToRemove) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);

        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getEmoticonUid4();

            if (existingMembers != null && existingMembers.length > 0) {
                List<String> updatedMembersList = new ArrayList<>(Arrays.asList(existingMembers));
                updatedMembersList.remove(uidToRemove);
                String[] updatedMembers = updatedMembersList.toArray(new String[0]);

                Update update = new Update().set("emoticonUid4", updatedMembers);
                mongoTemplate.updateFirst(query, update, Vote.class);
            }
        }
    }
    public void emotion5Member(String vid, String newUid) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);
        System.out.println(vid);
        System.out.println(newUid);
        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getEmoticonUid5();
            int existingMemberSize = existingMembers != null ? existingMembers.length : 0;
            boolean containsUid = false;
            if (existingMembers != null) {
                for (String memberUid : existingMembers) {
                    if (memberUid.equals(newUid)) {
                        containsUid = true;
                        emotionDeleteMember5(vid, newUid);
                        break;
                    }
                }
            }
            // 새로운 uid가 member에 포함되어 있지 않으면 추가
            if (!containsUid) {
                String[] newMembers = new String[existingMemberSize + 1];
                System.arraycopy(existingMembers, 0, newMembers, 0, existingMemberSize);
                newMembers[existingMemberSize] = newUid;

                Update update = new Update()
                        .set("emoticonUid5", newMembers);
                mongoTemplate.updateFirst(query, update, Vote.class);
            }
        }
    }
    /*이모2 삭제 */
    public void emotionDeleteMember5(String vid, String uidToRemove) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote existingDocument = mongoTemplate.findOne(query, Vote.class);

        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getEmoticonUid5();

            if (existingMembers != null && existingMembers.length > 0) {
                List<String> updatedMembersList = new ArrayList<>(Arrays.asList(existingMembers));
                updatedMembersList.remove(uidToRemove);
                String[] updatedMembers = updatedMembersList.toArray(new String[0]);

                Update update = new Update().set("emoticonUid5", updatedMembers);
                mongoTemplate.updateFirst(query, update, Vote.class);
            }
        }
    }
    /*여기서부터는 댓글이다 /////////////////////////////////////////////////////*/
    public void saveComment(Comment comment, String newCid, String Nickname, String Image) {
        comment.setCImage(Image);
        comment.setNickName(Nickname);
        comment.setCid(newCid);
        appCommentRepository.save(comment);
    }
    public List<Comment> CommentfindByCid(String cid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("cid").in(cid));
        return mongoTemplate.find(query, Comment.class);
    }
    public void CommentUpdate(String cid,String comment) {
        Query query = new Query(Criteria.where("cid").is(cid));
        Update update = new Update().set("comment",comment);
        mongoTemplate.updateMulti(query, update, Comment.class);
    }
    public void sendCommentMessage(String gid, String uid,String subject) throws FirebaseMessagingException {
        Query query2 = new Query(Criteria.where("uid").is(uid).and("gid").is(gid));
        UserGroup userGroup=mongoTemplate.findOne(query2, UserGroup.class);
        String Nickname=userGroup.getGroupNickname();
        Group groups=appGroupRepository.findByGid(gid);
        List<Userinfo> users=new ArrayList<>();
        String [] uids=groups.getUid();
        for (String id : uids) {
            if (!id.equals(uid)) {
                List<Userinfo> userList = UserinfoByUid(id);
                users.addAll(userList);
            }
        }
        List<String> fcmtoken=new ArrayList<>();
        for (Userinfo userinfo : users) {
            String token1= userinfo.getFcmToken();
            if (token1 != null && !token1.isEmpty()) {
                fcmtoken.add(token1);
            }
            System.out.println(userinfo.getFcmToken());
        }
        String title=subject;
        String body=Nickname+"님이 투표에 댓글을 남겼습니다. 지금 바로 확인하세요!";
        fcmService.sendPushs(title,body,fcmtoken);
    }
    public List<Comment> getComment(String vid){
        Query query = new Query(Criteria.where("vid").is(vid));
        return mongoTemplate.find(query, Comment.class);
    }
    public void complainCountUp(String cid){
        Query query = new Query(Criteria.where("cid").is(cid));
        Update update = new Update().inc("complainCount", 1);
        mongoTemplate.findAndModify(query, update, Comment.class);
    }
    public void CommentRemove(String cid) {
        Query query = new Query(Criteria.where("cid").is(cid));
        mongoTemplate.remove(query, Comment.class);
    }
    /* 여기부터는 피드백 구간*/
    public void saveFeedBack(FeedBack feedBack) {

       appFeedBackRepository.save(feedBack);
    }
}
