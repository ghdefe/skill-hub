package com.agentskills.sharing.repository;

import com.agentskills.sharing.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends JpaRepository<Repository, String> {

    List<Repository> findByUserId(String userId);

    Optional<Repository> findByUserIdAndGithubOwnerAndGithubRepo(
            String userId, String githubOwner, String githubRepo);

    boolean existsByUserIdAndGithubOwnerAndGithubRepo(
            String userId, String githubOwner, String githubRepo);

    List<Repository> findByGithubOwnerAndGithubRepo(String githubOwner, String githubRepo);

}
