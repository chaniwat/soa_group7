package com.shenzhentagram.controller;

import com.shenzhentagram.model.FollowIds;
import com.shenzhentagram.model.Follower;
import com.shenzhentagram.model.Following;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * Created by Meranote on 4/27/2017.
 */
@RestController
@CrossOrigin
@RequestMapping("/users")
public class FollowController extends TemplateRestController {

    public static final String USER_FOLLOW = "/users/{id}/follows";

    @Autowired
    private UserController userController;

    public FollowController(Environment environment, RestTemplateBuilder restTemplateBuilder) {
        super(environment, restTemplateBuilder, "follow");
    }

    @GetMapping("/{id}/follower")
    public ResponseEntity<Follower> getFollower(
            @PathVariable("id") int userId
    ) {
        ResponseEntity<FollowIds> responseEntity = request(HttpMethod.GET, USER_FOLLOW, FollowIds.class, userId);


        Follower follower = userController.convertFollowerIds(responseEntity.getBody().getFollower());

        return new ResponseEntity<>(follower, responseEntity.getStatusCode());
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<Following> getFollowing(
            @PathVariable("id") int userId
    ) {
        ResponseEntity<FollowIds> responseEntity = request(HttpMethod.GET, USER_FOLLOW, FollowIds.class, userId);

        Following following = userController.convertFollowingIds(responseEntity.getBody().getFollowing());

        return new ResponseEntity<>(following, responseEntity.getStatusCode());
    }

    @PostMapping("/{id}/follows")
    public ResponseEntity<Void> followTo(
            @PathVariable("id") int userId
    ) {
        HashMap<String, Object> sendPayload = new HashMap<>();
        sendPayload.put("userId", getAuthenticatedUser().getId());

        userController.increaseFollower(userId);

        userController.increaseFollowing(getAuthenticatedUser().getId());

        return request(HttpMethod.POST, USER_FOLLOW, sendPayload, Void.class, userId);
    }

    @DeleteMapping("/{id}/follows")
    public ResponseEntity<Void> unfollowTo(
            @PathVariable("id") int userId
    ) {
        HashMap<String, Object> sendPayload = new HashMap<>();
        sendPayload.put("userId", getAuthenticatedUser().getId());

        userController.decreaseFollower(userId);

        userController.decreaseFollowing(getAuthenticatedUser().getId());

        return request(HttpMethod.DELETE, USER_FOLLOW, sendPayload, Void.class, userId);
    }

}
