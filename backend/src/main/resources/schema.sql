-- ============================================================
-- SkillHub - PostgreSQL-specific objects
-- JPA handles entity tables via ddl-auto: update;
-- this script handles full-text search index (GIN + tsvector).
-- ============================================================

-- Full-text search index on skills table (name + description)
CREATE INDEX IF NOT EXISTS idx_skills_fts
    ON skills USING GIN (to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, '')));
