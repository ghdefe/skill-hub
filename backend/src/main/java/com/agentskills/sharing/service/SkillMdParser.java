package com.agentskills.sharing.service;

import com.agentskills.sharing.dto.SkillMdMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 解析 SKILL.md 文件内容中的 YAML front matter，提取 name 和 description 字段。
 * <p>
 * 这是一个无状态的工具类，不作为 Spring Bean 注册。
 */
public final class SkillMdParser {

    private static final Logger log = LoggerFactory.getLogger(SkillMdParser.class);
    private static final String DELIMITER = "---";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private SkillMdParser() {
        // utility class
    }

    /**
     * 解析 SKILL.md 内容中的 YAML front matter。
     *
     * @param content SKILL.md 文件的完整文本内容
     * @return 解析结果，包含 name 和 description（可能为 null）；
     *         对于无效输入返回 {@link SkillMdMetadata#EMPTY}
     */
    public static SkillMdMetadata parse(String content) {
        if (content == null || content.isBlank()) {
            return SkillMdMetadata.EMPTY;
        }

        // 跳过前导空白行，检查是否以 --- 开头
        String trimmed = content.stripLeading();
        if (!trimmed.startsWith(DELIMITER)) {
            return SkillMdMetadata.EMPTY;
        }

        // 找到第一个 --- 之后的位置
        int firstDelimiterEnd = trimmed.indexOf('\n', 0);
        if (firstDelimiterEnd == -1) {
            // 只有一行 "---"，没有结束分隔符
            return SkillMdMetadata.EMPTY;
        }

        // 查找第二个 --- 分隔符
        String afterFirst = trimmed.substring(firstDelimiterEnd + 1);
        int secondDelimiterIndex = findSecondDelimiter(afterFirst);
        if (secondDelimiterIndex == -1) {
            return SkillMdMetadata.EMPTY;
        }

        // 提取两个 --- 之间的 YAML 块
        String yamlBlock = afterFirst.substring(0, secondDelimiterIndex);

        return parseYamlBlock(yamlBlock);
    }

    /**
     * 在文本中查找独立行上的 --- 分隔符。
     *
     * @return 分隔符所在行的起始索引，未找到返回 -1
     */
    private static int findSecondDelimiter(String text) {
        int index = 0;
        while (index <= text.length() - DELIMITER.length()) {
            // 检查当前行是否为 ---（可能带尾部空白）
            int lineEnd = text.indexOf('\n', index);
            String line;
            if (lineEnd == -1) {
                line = text.substring(index);
            } else {
                line = text.substring(index, lineEnd);
            }

            if (line.strip().equals(DELIMITER)) {
                return index;
            }

            if (lineEnd == -1) {
                break;
            }
            index = lineEnd + 1;
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static SkillMdMetadata parseYamlBlock(String yamlBlock) {
        try {
            Map<String, Object> map = YAML_MAPPER.readValue(yamlBlock, Map.class);
            if (map == null) {
                return SkillMdMetadata.EMPTY;
            }

            String name = extractString(map, "name");
            String description = extractString(map, "description");
            return new SkillMdMetadata(name, description);
        } catch (Exception e) {
            log.warn("Failed to parse YAML front matter: {}", e.getMessage());
            return SkillMdMetadata.EMPTY;
        }
    }

    /**
     * 从 map 中提取字符串值；如果值不是 String 类型则忽略（返回 null）。
     */
    private static String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String s) {
            return s;
        }
        return null;
    }
}
