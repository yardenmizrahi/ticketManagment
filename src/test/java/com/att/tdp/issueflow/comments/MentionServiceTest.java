package com.att.tdp.issueflow.comments;

import com.att.tdp.issueflow.common.ApiException;
import com.att.tdp.issueflow.users.User;
import com.att.tdp.issueflow.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MentionServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    CommentMentionRepository mentionRepository;

    @InjectMocks
    MentionService mentionService;

    @Test
    void resolvesMentionsCaseInsensitivelyAndDeduplicates() {
        User user = user(1L, "jdoe");
        when(userRepository.findByLowercaseUsernames(Set.of("jdoe"))).thenReturn(List.of(user));

        List<User> users = mentionService.resolveMentionedUsers("Hi @JDoe and @jdoe");

        assertThat(users).extracting(User::getUsername).containsExactly("jdoe");
    }

    @Test
    void rejectsUnknownMentionedUsers() {
        when(userRepository.findByLowercaseUsernames(Set.of("missing"))).thenReturn(List.of());

        assertThatThrownBy(() -> mentionService.resolveMentionedUsers("Hi @missing"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown mentioned user");
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFullName("John Doe");
        return user;
    }
}
