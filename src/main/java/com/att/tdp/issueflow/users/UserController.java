package com.att.tdp.issueflow.users;

import com.att.tdp.issueflow.auth.CurrentUser;
import com.att.tdp.issueflow.comments.CommentDtos.MentionsPageResponse;
import com.att.tdp.issueflow.comments.CommentService;
import com.att.tdp.issueflow.users.UserDtos.CreateUserRequest;
import com.att.tdp.issueflow.users.UserDtos.UpdateUserRequest;
import com.att.tdp.issueflow.users.UserDtos.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final CurrentUser currentUser;
    private final CommentService commentService;

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{userId}")
    public UserResponse findById(@PathVariable Long userId) {
        return userService.findById(userId);
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request, currentUser.get());
    }

    @PostMapping("/update/{userId}")
    public void update(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        userService.update(userId, request, currentUser.get());
    }

    @DeleteMapping("/{userId}")
    public void delete(@PathVariable Long userId) {
        userService.delete(userId, currentUser.get());
    }

    @GetMapping("/{userId}/mentions")
    public MentionsPageResponse mentions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return commentService.findMentions(userId, page, pageSize);
    }
}
