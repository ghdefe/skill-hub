package com.agentskills.sharing.repository;

import com.agentskills.sharing.entity.Skill;
import com.agentskills.sharing.entity.SkillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, String> {

    List<Skill> findBySkillGroupId(String skillGroupId);

    List<Skill> findBySkillGroupIdAndStatus(String skillGroupId, SkillStatus status);

    Page<Skill> findByStatus(SkillStatus status, Pageable pageable);

    Optional<Skill> findByUserIdAndName(String userId, String name);

    boolean existsByUserIdAndName(String userId, String name);

    /**
     * Full-text search using PostgreSQL tsvector.
     */
    @Query(value = "SELECT s.* FROM skills s "
            + "WHERE to_tsvector('english', coalesce(s.name, '') || ' ' || coalesce(s.description, '')) "
            + "@@ plainto_tsquery('english', :query) "
            + "AND s.status = 'ACTIVE' "
            + "ORDER BY ts_rank(to_tsvector('english', coalesce(s.name, '') || ' ' || coalesce(s.description, '')), "
            + "plainto_tsquery('english', :query)) DESC",
            nativeQuery = true)
    List<Skill> searchByFts(@Param("query") String query);

    @Query("SELECT s FROM Skill s WHERE s.status = :status "
            + "AND s.id IN (SELECT sk.id FROM Skill sk JOIN sk.tags t WHERE t.name IN :tagNames "
            + "GROUP BY sk.id HAVING COUNT(DISTINCT t.name) = :tagCount)")
    Page<Skill> findByStatusAndTags(
            @Param("status") SkillStatus status,
            @Param("tagNames") List<String> tagNames,
            @Param("tagCount") long tagCount,
            Pageable pageable);
}
