package com.agentskills.sharing.repository;

import com.agentskills.sharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByGithubId(String githubId);

    Optional<User> findByUsername(String username);

    boolean existsByGithubId(String githubId);
}
