package com.agentskills.sharing.repository;

import com.agentskills.sharing.entity.SkillGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillGroupRepository extends JpaRepository<SkillGroup, String> {

    Optional<SkillGroup> findByRepositoryId(String repositoryId);

    List<SkillGroup> findByUserId(String userId);

    boolean existsByUserIdAndName(String userId, String name);

    Optional<SkillGroup> findByUserIdAndName(String userId, String name);
}
