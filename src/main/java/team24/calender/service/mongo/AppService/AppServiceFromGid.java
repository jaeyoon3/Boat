package team24.calender.service.mongo.AppService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import team24.calender.domain.Group.Group;

import team24.calender.domain.UserGroup.UserGroup;
import team24.calender.domain.Vote.Vote;
import team24.calender.error.DuplicateUidException;
import team24.calender.repository.AppRepo.AppGroupRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
@Service
public class AppServiceFromGid {

    private final MongoTemplate mongoTemplate;

    @Autowired
    AppGroupRepository appGroupRepository;

    public String CreateGid(){
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        String newGid = random.ints(leftLimit,rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return newGid;
    }


    public void saveGroup(String gid, String groupName, String groupCategory, String groupNotice, String groupImage) {
        String [] uid = new String[0];
        Group group1=new Group(gid,uid,groupName,groupCategory,groupNotice,groupImage);
        appGroupRepository.save(group1);// 새로운 gid 값
    }
    public List<Group> GroupfindByGid(String gid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("gid").in(gid));
        return mongoTemplate.find(query, Group.class);
    }
    public Group GroupOnefindByGid(String gid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("gid").in(gid));
        return appGroupRepository.findByGid(gid);
    }
    public void GroupUpdate(String gid,String groupName,String groupImage) {
        Query query = new Query(Criteria.where("gid").in(gid));
        Update update = new Update().set("groupName", groupName);
        Update update1 = new Update().set("groupImage", groupImage);
        mongoTemplate.updateMulti(query, update, Group.class);
        mongoTemplate.updateMulti(query, update1, Group.class);
    }
    public void GroupNotice(String gid,String groupNotice) {
        Query query = new Query(Criteria.where("gid").in(gid));
        Update update = new Update().set("groupNotice", groupNotice);
        mongoTemplate.updateMulti(query, update, Group.class);
    }
    public void DeleteGroupMember(String gid, String uidToRemove) {
        Query query = new Query(Criteria.where("gid").is(gid));
        Query query2 = new Query(Criteria.where("uid").is(uidToRemove).and("gid").is(gid));
        Group existingDocument = mongoTemplate.findOne(query, Group.class);
        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getUid();

            if (existingMembers != null && existingMembers.length > 0) {
                List<String> updatedMembersList = new ArrayList<>(Arrays.asList(existingMembers));
                updatedMembersList.remove(uidToRemove);
                String[] updatedMembers = updatedMembersList.toArray(new String[0]);

                Update update = new Update().set("uid", updatedMembers);
                mongoTemplate.updateFirst(query, update, Group.class);
                mongoTemplate.remove(query2, UserGroup.class);
            }
        }
    }
    public String UserGroupNicknameFind(String gid, String uid){
        Query query = new Query(Criteria.where("uid").is(uid).and("gid").is(gid));
        UserGroup findUser= mongoTemplate.findOne(query, UserGroup.class);
        return findUser.getGroupNickname();
    }
    public String UserGroupImageFind(String gid, String uid){
        Query query = new Query(Criteria.where("uid").is(uid).and("gid").is(gid));
        UserGroup findUser= mongoTemplate.findOne(query, UserGroup.class);
        return findUser.getImageUrl();
    }
    public void GroupRemove(String gid) {
        Query query = new Query(Criteria.where("gid").is(gid));
        mongoTemplate.remove(query, Group.class);
    }

    public void UserGroupInsert(UserGroup userGroup){mongoTemplate.insert(userGroup);}

    public void UserGroupUpdate(String uid,String gid,String groupNickname,String groupRoll,String groupPersonalEmail,String groupPersonalPhone,String groupPersonalAddress) {
        Query query = new Query(Criteria.where("uid").is(uid).and("gid").is(gid));
        Update update = new Update().set("groupNickname", groupNickname);
        Update update1 = new Update().set("groupRoll", groupRoll);
        Update update2 = new Update().set("groupPersonalEmail", groupPersonalEmail);
        Update update3 = new Update().set("groupPersonalPhone", groupPersonalPhone);
        Update update4 = new Update().set("groupPersonalAddress", groupPersonalAddress);
        mongoTemplate.updateMulti(query, update, UserGroup.class);
        mongoTemplate.updateMulti(query, update1, UserGroup.class);
        mongoTemplate.updateMulti(query, update2, UserGroup.class);
        mongoTemplate.updateMulti(query, update3, UserGroup.class);
        mongoTemplate.updateMulti(query, update4, UserGroup.class);
    }
    public void UserGroupImageUpdate(String uid, String image) {
        Query query = new Query(Criteria.where("uid").is(uid));
        Update update = new Update().set("imageUrl", image);
        mongoTemplate.updateMulti(query, update, UserGroup.class);
    }
    public List<UserGroup> UserGroupfindByGid(String gid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("gid").in(gid));
        return mongoTemplate.find(query, UserGroup.class);
    }
    public void UserGroupRemove(String gid) {
        Query query = new Query(Criteria.where("gid").is(gid));
        mongoTemplate.remove(query, UserGroup.class);
    }
    public List<Vote> VotefindByGid(String gid) {
        Query query = new Query();
        query.addCriteria(Criteria.where("gid").in(gid));
        return mongoTemplate.find(query, Vote.class);
    }


    /*그룹에 uid추가*/
    public void groupMember(String gid, String newUid) {
        Query query = new Query(Criteria.where("gid").is(gid));
        Group existingDocument = mongoTemplate.findOne(query, Group.class);
        System.out.println(gid);
        System.out.println(newUid);
        if (existingDocument != null) {
            String[] existingMembers = existingDocument.getUid();
            int existingMemberSize = existingMembers != null ? existingMembers.length : 0;
            // 새로운 uid가 이미 member에 포함되어 있는지 확인
            boolean containsUid = false;
            if (existingMembers != null) {
                for (String member : existingMembers) {
                    if (member.equals(newUid)) {
                        throw new DuplicateUidException("uid 중복", 1000);
                    }
                }
            }


            // 새로운 uid가 member에 포함되어 있지 않으면 추가
            if (!containsUid) {
                String[] newMembers = new String[existingMemberSize + 1];
                System.arraycopy(existingMembers, 0, newMembers, 0, existingMemberSize);
                newMembers[existingMemberSize] = newUid;

                Update update = new Update()
                        .set("uid", newMembers);
                mongoTemplate.updateFirst(query, update, Group.class);
            }
        }
        else{
            throw new RuntimeException("gid가 존재하지 않습니다");
        }
    }


}
