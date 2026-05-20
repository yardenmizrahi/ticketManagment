package com.att.tdp.issueflow.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleOrderByCreatedAtAscIdAsc(Role role);

    @Query("select u from User u where lower(u.username) in :usernames")
    List<User> findByLowercaseUsernames(Set<String> usernames);
}
