package team24.calender.repository.AppRepo;

import org.springframework.data.mongodb.repository.MongoRepository;

import team24.calender.domain.Group.Group;



public interface AppGroupRepository extends MongoRepository<Group, String> {
    Group findByGid(String gid);
}
