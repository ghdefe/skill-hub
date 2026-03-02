package com.agentskills.sharing.dto;

/**
 * 从 SKILL.md 文件的 YAML front matter 中解析出的元数据。
 *
 * @param name        Skill 名称，可为 null
 * @param description Skill 描述，可为 null
 */
public record SkillMdMetadata(
        String name,
        String description
) {

    /** 空的解析结果，name 和 description 均为 null。 */
    public static final SkillMdMetadata EMPTY = new SkillMdMetadata(null, null);

    /**
     * 判断是否包含有效的名称。
     */
    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    /**
     * 判断是否包含有效的描述。
     */
    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }
}
