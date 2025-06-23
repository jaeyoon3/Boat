package team24.calender.controller.App;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import team24.calender.domain.ALL;

import team24.calender.domain.FeedBack.FeedBack;
import team24.calender.domain.Group.Group;
import team24.calender.domain.Holiday.Holiday;
import team24.calender.domain.Schedule.Schedule;
import team24.calender.domain.UserGroup.UserGroup;
import team24.calender.domain.Vote.Comment;
import team24.calender.domain.Vote.Vote;


import team24.calender.domain.user.Userinfo;
import team24.calender.repository.AppRepo.AppCommentRepository;
import team24.calender.repository.AppRepo.AppGroupRepository;
import team24.calender.repository.AppRepo.AppUserRepository;
import team24.calender.service.mongo.AppService.*;

import team24.calender.service.mongo.S3FileUploadTestService;


import java.awt.desktop.ScreenSleepEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

@RequiredArgsConstructor
@RestController
@Slf4j
public class AppController {

    private final AppService appService;
    private final AppServiceFromGid appServiceFromGid;

    private final Scheduling scheduling;

    private final S3FileUploadTestService s3FileUploadTestService;

    private final OpenApiService openApiService;

    private final LongPolling longPolling;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<String, DeferredResult<List<Vote>>> deferredResults = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Vote>> previousVotesMap = new ConcurrentHashMap<>();

    @Autowired
    AppGroupRepository appGroupRepository;
    @Autowired
    AppCommentRepository appCommentRepository;
    @Autowired
    AppUserRepository appUserRepository;
    /*
    @GetMapping("/auth")
    public List<String> getAllUsers() {
        List<String> userInfoList = new ArrayList<>();
        log.info("auth find");
        try {
            ListUsersPage page = FirebaseAuth.getInstance().listUsers(null);
            for (UserRecord user : page.iterateAll()) {
                String uid=user.getUid();
                userInfoList.add("Uid: "+uid);
            }
        } catch (FirebaseAuthException e) {
            e.printStackTrace();

        }
        return userInfoList;
    }
    */
    @PostMapping("/holiday")
    public String holidayGet() throws JSONException, IOException {
        openApiService.getHolidayService();
        return "holiday";
    }
    @GetMapping("/getHoliday")
    public List<Holiday> holiday() {
        return openApiService.holidayGet();
    }
    @PostMapping("/userinfoSave")
    public String userSave(@RequestBody Userinfo userinfo){
        appService.AppUserInsert(userinfo);
        return "user";
    }
    @GetMapping("/userinfoGet")
    public Map<String, String> userinfoGet(@RequestParam("uid") String uid){
        return appService.response(uid);
    }
    @PutMapping("/userInfoUpdate")
    public String userUpdate(@RequestParam("uid") String uid,@RequestParam("image") MultipartFile image) throws IOException {
        String preUrl=appUserRepository.findByUid(uid).getImage();
        System.out.println(preUrl);
        if(preUrl.isEmpty()){
            String imageUrl=s3FileUploadTestService.uploadUserFile(image);
            appService.UserImageUpdate(uid,imageUrl);
            appServiceFromGid.UserGroupImageUpdate(uid,imageUrl);
        }
        else {
            s3FileUploadTestService.deleteFile("UserImage/"+preUrl.split("/")[4]);
            String imageUrl=s3FileUploadTestService.uploadUserFile(image);
            appService.UserImageUpdate(uid,imageUrl);
            appServiceFromGid.UserGroupImageUpdate(uid,imageUrl);
        }
        return "image";
    }
    @PutMapping("/userInfoNicknameUpdate")
    public String usernickNameUpdate(@RequestParam("uid") String uid,@RequestParam("nickName") String nickName)  {
        appService.UserNicknameUpdate(uid,nickName);
        return "nickName";
    }
    @PutMapping("/userInfoTokenUpdate")
    public String userTokenUpdate(@RequestParam("uid") String uid,@RequestParam("fcmToken") String fcmToken)  {
        appService.UserTokenUpdate(uid,fcmToken);
        return "fcmToken";
    }
    @DeleteMapping(value = "/userLeave")
    public String userLeave(@RequestParam("uid") String uid){
        appService.UserRemove(uid);
        return "userLeave";
    }
    /* 그룹 생성 */
    @PostMapping("/groupsave")
    public String GroupSave(@RequestParam("uid") String uid,@RequestParam("groupName") String groupName,@RequestParam("groupCategory") String groupCategory,@RequestParam("groupNotice") String groupNotice,@RequestParam("groupImage") MultipartFile groupImage) throws IOException {
        String newGid;
        List<Group> group2;
        String nickname=appUserRepository.findByUid(uid).getNickName();
        do {
            newGid = appServiceFromGid.CreateGid();
            group2 = appServiceFromGid.GroupfindByGid(newGid);
        } while (group2 == null);
        String imageUrl=s3FileUploadTestService.uploadFile(groupImage);
        appServiceFromGid.saveGroup(newGid,groupName,groupCategory,groupNotice,imageUrl);
        String image=appUserRepository.findByUid(uid).getImage();
        UserGroup userGroup=new UserGroup(uid,newGid,nickname,"","","","",image);
        appServiceFromGid.UserGroupInsert(userGroup);
        appServiceFromGid.groupMember(newGid,uid);
        return "image"+imageUrl;
    }
    /* 그룹 업데이트 */
    @PutMapping("/groupUpdate")
    public String GroupUpdate(@RequestParam("gid") String gid,@RequestParam("groupName") String groupName,@RequestParam("groupImage") MultipartFile groupImage) throws IOException {
        String preUrl=appGroupRepository.findByGid(gid).getGroupImage();
        if(preUrl.isEmpty()){
            String imageUrl=s3FileUploadTestService.uploadFile(groupImage);
            appServiceFromGid.GroupUpdate(gid,groupName,imageUrl);
        }
        else {
            s3FileUploadTestService.deleteFile("image/"+preUrl.split("/")[4]);
            String imageUrl = s3FileUploadTestService.uploadFile(groupImage);
            appServiceFromGid.GroupUpdate(gid,groupName,imageUrl);
        }
        return "image";
    }
    /* 그룹 Notice 수정 */
    @PutMapping("/groupNotice")
    public String GroupCategory(@RequestParam("gid") String gid,@RequestParam("groupNotice") String groupNotice){
        appServiceFromGid.GroupNotice(gid,groupNotice);
        return "Notice";
    }
    /* 그룹 나가기 */
    @PutMapping(value = "/groupLeave")
    public String groupLeave(@RequestParam("gid") String gid,@RequestParam("uid") String uid){
        appServiceFromGid.DeleteGroupMember(gid,uid);
        return "groupLeave";
    }
    /* 그룹 삭제 */
    @DeleteMapping("/groupDelete")
    public String groupDelete(@RequestParam("gid") String gid) throws IOException {
        String preUrl=appGroupRepository.findByGid(gid).getGroupImage();
        s3FileUploadTestService.deleteFile("image/"+preUrl.split("/")[4]);
        appServiceFromGid.GroupRemove(gid);
        appServiceFromGid.UserGroupRemove(gid);
        return "groupDelete";
    }
    /* 일정 생성 */
    @PostMapping("/Schedulesave")
    public String ScheduleSave(@RequestBody Schedule schedule){
        String newSid;
        List<Schedule> schedules;
        do{
            newSid= schedule.getUid()+appService.CreateSid();
            schedules= appService.ScheduleFindBySid(newSid);
        } while (schedules==null);

        appService.saveSchedule(schedule,newSid);

        LocalDateTime startDate= schedule.getScheduleStartDate().minusMinutes(30);
        int startYear=startDate.getYear();
        int startMonth=startDate.getMonthValue();
        int startDay=startDate.getDayOfMonth();
        int startHour=startDate.getHour();
        int startMin=startDate.getMinute();
        LocalDateTime now= LocalDateTime.now();
        int Year=now.getYear();
        int Month=now.getMonthValue();
        int Day=now.getDayOfMonth();
        int Hour=now.getHour();
        int Min=now.getMinute();
        if(startYear>=Year){
            if(startMonth>=Month){
                if(startDay>=Day){
                    if(startHour>=Hour){
                        if (startMin>Min){
                            scheduling.scheduleTask3(newSid);
                            System.out.println("스케쥴링 시작");
                        }
                    }
                }
            }
        }

        return "schedule";
    }
    /* 일정 수정 */
    @PutMapping("/ScheduleUpdate")
    public String uploadImage(@RequestParam("uid") String uid, @RequestParam("sid") String sid, @RequestParam("scheduleSubject") String scheduleSubject, @RequestParam("scheduleContents") String scheduleContents, @RequestParam("scheduleAddress") String scheduleAddress, @RequestParam("scheduleStartDate") LocalDateTime scheduleStartDate,@RequestParam("scheduleEndDate") LocalDateTime scheduleEndDate,@RequestParam("color") String color) {
        appService.ScheduleUpdate(uid,sid,scheduleSubject,scheduleContents,scheduleAddress,scheduleStartDate,scheduleEndDate,color);
        return "ScheduleUpdate";
    }
    @PutMapping("/ScheduleCheck")
    public String ScheduleCheck(@RequestParam("sid") String sid){
        appService.ScheduleCheck(sid);
        return "scheduleCheck";
    }
    /* 일정 제거 */
    @DeleteMapping("/ScheduleDelete")
    public String scheduleDelete(@RequestParam("sid") String sid){
        appService.ScheduleRemove(sid);
        return "scheduleDelete";
    }
    @GetMapping("/ScheduleGet")
    public List<Schedule> ScheduleGet(@RequestParam("uid") String uid){
        System.out.println("schedule2222");
        return appService.SchedulefindByUid(uid);
    }
    @GetMapping("/voteGid")
    public List<Vote> voteGid(@RequestParam("gid") String gid){
        return appService.VotefindByGid(gid);
    }
    /* 투표 생성 */
    @PostMapping("/Votesave")
    public String VoteSave(@RequestBody Vote vote) throws FirebaseMessagingException {
        String newVid;
        List<Vote> votes;
        do{
            newVid= vote.getGid()+appService.CreateVid();
            votes= appService.VoteFindByVid(newVid);
        } while (votes==null);
        String organizer=vote.getOrganizer();
        String gid=vote.getGid();
        String groupName=appServiceFromGid.GroupOnefindByGid(gid).getGroupName();
        String nickname=appServiceFromGid.UserGroupNicknameFind(gid,organizer);
        String image= appServiceFromGid.UserGroupImageFind(gid,organizer);
        appService.saveVote(vote,newVid,nickname,image,groupName);

        String subject=vote.getSubject();
        String gidFromVid= vote.getGid();
        appService.sendStartMessage(gidFromVid,organizer,subject);
        boolean firstCome=vote.isFirstCome();

        if(firstCome){
            System.out.println(firstCome+"//////////////////////////////////");
            scheduling.scheduleTask2(newVid,organizer,subject);
        }
        else {
            System.out.println(firstCome+"//////////////////////////////////");
            scheduling.scheduleTask(newVid, organizer, subject);
        }
        return "Vote";
    }
    /*
    @PostMapping("/firstComeVote")
    public DeferredResult<Boolean> checkVote(@RequestBody Vote vote) throws FirebaseMessagingException {
        String newVid;
        List<Vote> votes;
        do{
            newVid= vote.getGid()+appService.CreateVid();
            votes= appService.VoteFindByVid(newVid);
        } while (votes==null);
        String organizer=vote.getOrganizer();
        String gid=vote.getGid();
        String groupName=appServiceFromGid.GroupOnefindByGid(gid).getGroupName();
        String nickname=appServiceFromGid.UserGroupNicknameFind(gid,organizer);
        String image= appServiceFromGid.UserGroupImageFind(gid,organizer);
        appService.saveVote(vote,newVid,nickname,image,groupName);

        String subject=vote.getSubject();
        String gidFromVid= vote.getGid();
        appService.sendStartMessage(gidFromVid,organizer,subject);

        DeferredResult<Boolean> deferredResult = new DeferredResult<>(60000L);
        longPolling.checkVote(newVid, organizer,subject,deferredResult);
        deferredResult.onTimeout(() -> deferredResult.setErrorResult("Timeout"));
        return deferredResult;
    }

     */
    @GetMapping(value = "/voteGet")
    public DeferredResult<List<Vote>> voteGet(@RequestParam("gid") String gid,@RequestParam("uid") String uid) {
        if (Objects.equals(gid, "") || Objects.equals(uid, "")) {
            System.out.println("bad request"+uid+"//"+gid+"//");
            throw new IllegalArgumentException("gid 또는 uid가 빈 문자열입니다.");
        }
        DeferredResult<List<Vote>> deferredResult = new DeferredResult<>(TimeUnit.MINUTES.toMillis(1));

        deferredResult.onTimeout(() -> {
            deferredResult.setErrorResult("Timeout occurred");
            stopScheduledTask(gid);
            deferredResults.remove(gid);
            System.out.println("Long polling request for gid " + gid + " timed out"+ uid);
        });

        deferredResult.onCompletion(() -> {
            stopScheduledTask(gid);
            deferredResults.remove(gid);
            System.out.println("Long polling request for gid " + gid + " completed" + uid);
        });

        deferredResults.put(gid, deferredResult);

        // 주기적으로 데이터 변경을 체크하는 작업을 실행
        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(() -> checkAndReturnVotes(gid,uid), 0, 10, TimeUnit.SECONDS);
        scheduledTasks.put(gid, scheduledTask);

        return deferredResult;
    }

    private void checkAndReturnVotes(String gid, String uid) {
        System.out.println("checkcheckcheck"+ uid);
        List<Vote> currentVotes = appService.VotefindByGid(gid);
        List<Vote> previousVotes = previousVotesMap.get(uid);

        if (previousVotes == null || !previousVotes.equals(currentVotes)) {
            previousVotesMap.put(uid, currentVotes);
            DeferredResult<List<Vote>> deferredResult = deferredResults.get(gid);
            if (deferredResult != null) {
                deferredResult.setResult(currentVotes);
            }
        }
    }

    private void stopScheduledTask(String gid) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(gid);
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
            scheduledTasks.remove(gid);
        }
    }

    /* 투표 제거 */
    @DeleteMapping("/VoteDelete")
    public String VoteDelete(@RequestParam("vid") String vid){
        scheduling.cancelScheduledTask(vid);
        appService.VoteRemove(vid);
        return "VoteDelete";
    }
    @PutMapping(value = "/vidagree2")
    public String voteAgree2(@RequestParam("vid") String vid,@RequestParam("uid") String uid) throws FirebaseMessagingException {
        appService.AgreeVoteMember(vid,uid);
        appService.MaxAgree(vid);
        return "member";
    }
    @PutMapping(value = "/vidagree")
    public String voteAgree(@RequestParam("vid") String vid,@RequestParam("uid") String uid) throws FirebaseMessagingException {
        Vote vote=appService.OneVotefindByVid(vid);
        String organizer=vote.getOrganizer();
        String subject=vote.getSubject();
        appService.AgreeVoteMember(vid,uid);
        appService.checkAgree(vid,organizer,subject);
        return "member";
    }
    @PutMapping(value = "/vidDisagree")
    public String voteDisagree(@RequestParam("vid") String vid,@RequestParam("uid") String uid){
        appService.disagreeVoteMember(vid,uid);
        return "member";
    }

    @PutMapping(value = "/pet")
    public String emotion1(@RequestParam("vid") String vid,@RequestParam("uid") String uid){
        appService.emotion1Member(vid,uid);
        return "member";
    }
    @PutMapping(value = "/deal")
    public String emotion2(@RequestParam("vid") String vid,@RequestParam("uid") String uid){
        appService.emotion2Member(vid,uid);
        return "member";
    }
    @PutMapping(value = "/gun")
    public String emotion3(@RequestParam("vid") String vid,@RequestParam("uid") String uid){
        appService.emotion3Member(vid,uid);
        return "member";
    }
    @PutMapping(value = "/jam")
    public String emotion4(@RequestParam("vid") String vid,@RequestParam("uid") String uid){
        appService.emotion4Member(vid,uid);
        return "member";
    }
    @PutMapping(value = "/jammies")
    public String emotion5(@RequestParam("vid") String vid,@RequestParam("uid") String uid){
        appService.emotion5Member(vid,uid);
        return "member";
    }
    @PostMapping("/CommentSave")
    public String CommentSave(@RequestBody Comment comment) throws FirebaseMessagingException {
        String newCid;
        List<Comment> comments;
        do{
            newCid= comment.getVid()+"comment"+appService.CreateVid();
            comments= appService.CommentfindByCid(newCid);
        } while (comments==null);
        Vote vote=appService.OneVotefindByVid(comment.getVid());
        String uid=comment.getUid();
        String gid=comment.getGid();
        String nickname=appServiceFromGid.UserGroupNicknameFind(gid,uid);
        String image= appServiceFromGid.UserGroupImageFind(gid,uid);
        appService.saveComment(comment,newCid,nickname,image);
        String subject=vote.getSubject();
        appService.sendCommentMessage(gid,uid,subject);

        return "COMMENT";
    }
    @PutMapping("/CommentUpdate")
    public String CommentSave(@RequestParam("cid") String cid,@RequestParam("comment") String comment) {
        appService.CommentUpdate(cid,comment);
        return "COMMENTUpdate";
    }
    @GetMapping("/CommentGet")
    public List<Comment> GetComment(@RequestParam("vid") String vid){
        return appService.getComment(vid);
    }
    @PutMapping("/countInc")
    public String countUp(@RequestParam("cid") String cid){
        appService.complainCountUp(cid);
        return "count";
    }
    @DeleteMapping(value = "/commentDel")
    public String commnetdel(@RequestParam("cid") String cid){
        appService.CommentRemove(cid);
        return "Comment";
    }
    /* 그룹에 인원 추가 */
    @PutMapping(value = "/uidinsert")
    public String memberInsert(@RequestParam("gid") String gid,@RequestParam("uid") String uid,@RequestParam("groupNickname")String groupNickname) {
        Userinfo user=appUserRepository.findByUid(uid);
        String imageUrl=user.getImage();
        UserGroup userGroup=new UserGroup(uid,gid,groupNickname,"","","","",imageUrl);
        appServiceFromGid.groupMember(gid,uid);
        appServiceFromGid.UserGroupInsert(userGroup);
        return "member";
    }
    /* 유저 그룹 수정 */
    @PutMapping("/UserGroupUpdate")
    public String UserGroupupload(@RequestParam("uid") String uid, @RequestParam("gid") String gid, @RequestParam("groupNickname") String groupNickname,@RequestParam("groupRoll") String groupRoll,@RequestParam("groupPersonalEmail") String groupPersonalEmail, @RequestParam("groupPersonalPhone") String groupPersonalPhone,@RequestParam("groupPersonalAddress") String groupPersonalAddress) {
        appServiceFromGid.UserGroupUpdate(uid,gid,groupNickname,groupRoll,groupPersonalEmail,groupPersonalPhone,groupPersonalAddress);
        return "upload";
    }

    /*피드백 전송 */
    @PostMapping("/feedback")
    public String FeedBack(@RequestBody FeedBack feedBack){
        appService.saveFeedBack(feedBack);
        return "feedback";
    }

    /*
    @GetMapping(value = "/appgroupres")
    public List<Group> groupResponse(@RequestParam("uid") String uid) {

        return appService.GroupfindByUid(uid);
    }
    @GetMapping(value = "/appScheduleres")
    public List<Schedule> ScheduleResponse(@RequestParam("uid") String uid) {

        return appService.SchedulefindByUid(uid);
    }
    @GetMapping(value = "/appUserGroupgroupres")
    public List<UserGroup> UserGroupResponse(@RequestParam("uid") String uid) {

        return appService.UserGroupfindByUid(uid);
    }
    */
    /* uid기준 정보 다보내는거*/
    @GetMapping(value = "/appres2")
    public ResponseEntity<?> uidresponse(@RequestParam("uid") String uid) {
        List<Group> groups = appService.GroupfindByUid(uid);
        List<Schedule> schedules = appService.SchedulefindByUid(uid);

        List<String> gidList = new ArrayList<>();
        for (Group group : groups) {
            gidList.add(group.getGid());
        }
        List<Vote> votes = new ArrayList<>();
        List<UserGroup> userGroups = new ArrayList<>();

        for (String gid : gidList) {
            List<Vote> voteList = appService.VotefindByGid(gid);
            votes.addAll(voteList);
            List<UserGroup> userGroupList = appServiceFromGid.UserGroupfindByGid(gid);
            userGroups.addAll(userGroupList);

        }
        ALL responseData = new ALL(groups, schedules, userGroups,votes);
        return ResponseEntity.ok(responseData);
    }

    /* gid기준 정보 다보내는거
    @GetMapping(value = "/groupRes")
    public ResponseEntity<?> gidResponse(@RequestParam("gid") String gid) {
        List<Group> groups = appServiceFromGid.GroupfindByGid(gid);
        List<Vote> votes = appServiceFromGid.VotefindByGid(gid);
        List<UserGroup> userGroups = appServiceFromGid.UserGroupfindByGid(gid);
        AllFromGid responseData = new AllFromGid(groups,userGroups,votes);
        return ResponseEntity.ok(responseData);
    }
    @PostMapping("/appsave")
    public String test(@RequestBody User user) {
        appService.AppUserInsert(user);
        return "test";
    }
    */


}
