package com.att.tdp.issueflow.comments;

import com.att.tdp.issueflow.common.ApiException;
import com.att.tdp.issueflow.users.User;
import com.att.tdp.issueflow.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MentionService {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_]+)");

    private final UserRepository userRepository;
    private final CommentMentionRepository mentionRepository;

    public List<User> resolveMentionedUsers(String content) {
        Set<String> usernames = extractLowercaseUsernames(content);
        if (usernames.isEmpty()) {
            return List.of();
        }
        List<User> users = userRepository.findByLowercaseUsernames(usernames);
        Set<String> found = users.stream()
                .map(user -> user.getUsername().toLowerCase())
                .collect(Collectors.toSet());
        Set<String> missing = usernames.stream().filter(username -> !found.contains(username)).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown mentioned user(s): " + String.join(", ", missing));
        }
        return users;
    }

    public void replaceMentions(Comment comment, List<User> users) {
        mentionRepository.deleteByCommentId(comment.getId());
        mentionRepository.flush();
        for (User user : users) {
            CommentMention mention = new CommentMention();
            mention.setComment(comment);
            mention.setUser(user);
            mentionRepository.save(mention);
        }
    }

    private Set<String> extractLowercaseUsernames(String content) {
        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content == null ? "" : content);
        while (matcher.find()) {
            usernames.add(matcher.group(1).toLowerCase());
        }
        return usernames;
    }
}
