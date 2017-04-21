package follow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/users")
public class FollowController {

    // For Annotation
    ApplicationContext ctx = new AnnotationConfigApplicationContext(SpringMongoConfig.class);
    MongoOperations mongoOperation = (MongoOperations) ctx.getBean("mongoTemplate");

    @Autowired
    private FollowByRepository followByRepository;
    @Autowired
    private FollowingRepository followingRepository;

    @RequestMapping(path = "/createdata", method = RequestMethod.GET)
    public void postCreateData() {
        ArrayList<String> followby = new ArrayList<>();
        followby.add("4");
        followby.add("2");
        followby.add("3");

        ArrayList<String> following = new ArrayList<String>();
        following.add("4");
        following.add("2");
        following.add("3");

        followByRepository.save(new FollowBy(1, followby));
        followingRepository.save(new Following(1,following));

    }

    @GetMapping("/{id}/follows")
    public Following getFollows(@PathVariable("id") Long id) {

        Following following = followingRepository.findByUserId(id);
        return following;

    }

    @GetMapping("/{id}/followed_by")
    public FollowBy getFollowed_by    (@PathVariable("id") Long id) {

        FollowBy followby = followByRepository.findByUserId(id);
        return followby;

    }

    @PostMapping("/{id}/followed_by")
    public FollowBy createFollowed_by   (@PathVariable("id") String id, @RequestBody Map<String, Object> payload) {
        ArrayList<String> users = new ArrayList<>();
        FollowBy followby;
        followby = followByRepository.findByUserId((int)payload.get("userId"));
        try {
            users = followby.getUsers();

            

            users.add(id);
            followby.setUsers(users);
            followByRepository.save(followby);
        }catch (Exception e){
            users.add(id);
            followby = new FollowBy((int)payload.get("userId") ,users);
            followByRepository.save(followby);
        }

        return followby;

    }

    @PostMapping("/{id}/follows")
    public Following createFollows   (@PathVariable("id") String id, @RequestBody Map<String, Object> payload) {
        ArrayList<String> users = new ArrayList<>();
        Following following;
        following = followingRepository.findByUserId((int)payload.get("userId"));
        try {
            users = following.getUsers();
            users.add(id);
            following.setUsers(users);
            followingRepository.save(following);
        }catch (Exception e){
            users.add(id);
            following = new Following((int)payload.get("userId") ,users);
            followingRepository.save(following);
        }

        return following;

    }

}
