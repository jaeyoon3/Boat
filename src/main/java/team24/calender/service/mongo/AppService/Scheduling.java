package team24.calender.service.mongo.AppService;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import team24.calender.domain.Group.Group;
import team24.calender.domain.Schedule.Schedule;
import team24.calender.domain.Vote.Vote;
import team24.calender.domain.user.Userinfo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;


@Component
@RequiredArgsConstructor
@Service
public class Scheduling {
    @Autowired
    private TaskScheduler taskScheduler;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    private final MongoTemplate mongoTemplate;
    private final AppService appService;

    public void scheduleTask(String vid, String organizer, String subject) {
        // 동적으로 cron 표현식 생성

        try {
            String cronExpression = generateCronExpression(vid);
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(() -> {
                try {
                    taskToExecute(vid,organizer,subject);
                } catch (FirebaseMessagingException e) {
                    throw new RuntimeException(e);
                }
            }, new CronTrigger(cronExpression));
            scheduledTasks.put(vid, scheduledTask);

        } catch (Exception ignored) {
        }
    }
    public void scheduleTask2(String vid, String organizer, String subject) {
        // 동적으로 cron 표현식 생성

        try {
            String cronExpression = generateCronExpression2();
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(() -> {
                try {
                    taskToExecute2(vid,organizer,subject);
                } catch (FirebaseMessagingException e) {
                    throw new RuntimeException(e);
                }
            }, new CronTrigger(cronExpression));
            scheduledTasks.put(vid, scheduledTask);

        } catch (Exception ignored) {
        }
    }
    public void scheduleTask3(String sid) {
        // 동적으로 cron 표현식 생성

        try {
            String cronExpression = generateCronExpression3(sid);
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(() -> {
                try {
                    taskToExecute3(sid);
                } catch (FirebaseMessagingException e) {
                    throw new RuntimeException(e);
                }
            }, new CronTrigger(cronExpression));
            scheduledTasks.put(sid, scheduledTask);

        } catch (Exception ignored) {
        }
    }
    public void cancelScheduledTask(String vid) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(vid);
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
            scheduledTasks.remove(vid);
            System.out.println("remove");
        }
    }

    private void taskToExecute(String vid, String organizer, String subject) throws FirebaseMessagingException {
        // 스케줄되는 작업 내용

        Query query = new Query(Criteria.where("vid").is(vid));
        Vote Document = mongoTemplate.findOne(query, Vote.class);
        int max= Document.getMax();
        boolean passed = Document.getAgreeUid().length >= max;
        appService.sendEndMessage(vid, organizer, subject);
        if (passed) {
            Update update = new Update().set("passed", passed);
            mongoTemplate.updateMulti(query, update, Vote.class);
        }
        else{
            mongoTemplate.remove(query, Vote.class);
        }
        cancelScheduledTask(vid);
    }
    private void taskToExecute2(String vid, String organizer, String subject) throws FirebaseMessagingException {
        // 스케줄되는 작업 내용

        Query query = new Query(Criteria.where("vid").is(vid));
        Vote Document = mongoTemplate.findOne(query, Vote.class);
        boolean passed = Document.isPassed();
        if (!passed) {
            appService.sendEndMessage(vid, organizer, subject);
            mongoTemplate.remove(query, Vote.class);
        }
        cancelScheduledTask(vid);
    }
    private void taskToExecute3(String sid) throws FirebaseMessagingException {
        // 스케줄되는 작업 내용
        appService.sendScheduleMessage(sid);
        System.out.println("Scheduled message " );
        cancelScheduledTask(sid);

    }

    private String generateCronExpression(String vid) {
        Query query = new Query(Criteria.where("vid").is(vid));
        Vote Document = mongoTemplate.findOne(query, Vote.class);
        LocalDateTime voteTimer = Document.getEndTime();
        int min = voteTimer.getMinute();
        int hour = voteTimer.getHour();
        int day = voteTimer.getDayOfMonth();
        int month = voteTimer.getMonthValue();

        System.out.println("Scheduled task executed for vote ID: " + vid);
        // cron 표현식 생성
        return String.format("0 %d %d %d %d *", min, hour, day, month);
    }
    private String generateCronExpression3(String sid) {
        Query query = new Query(Criteria.where("sid").is(sid));
        Schedule Document = mongoTemplate.findOne(query, Schedule.class);
        LocalDateTime voteTimer = Document.getScheduleStartDate().minusMinutes(30);
        int min = voteTimer.getMinute();
        int hour = voteTimer.getHour();
        int day = voteTimer.getDayOfMonth();
        int month = voteTimer.getMonthValue();

        System.out.println("Scheduled task executed for scedule ID: " + sid+ "////"+ min+ hour+ day+ month);
        // cron 표현식 생성
        return String.format("0 %d %d %d %d *", min, hour, day, month);
    }
    /*1일뒤 삭제위한 cron식*/
    private String generateCronExpression2() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledTime = now.plusDays(1);
        int min = scheduledTime.getMinute();
        int hour = scheduledTime.getHour();
        int day = scheduledTime.getDayOfMonth();
        int month = scheduledTime.getMonthValue();

        System.out.println("Scheduled task executed for vote ID: " );
        // cron 표현식 생성
        return String.format("0 %d %d %d %d *", min, hour, day, month);
    }

}
