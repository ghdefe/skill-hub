# 🔥 SkillHub — AI Agent 技能共享平台正式上线！

> 🤖 本项目由 **Kiro + Claude Opus 4.6** 全程 AI 生成，从架构设计到前后端代码，全程没有手写一行代码，纯 AI 驱动开发。

大家好！我用 AI 从零搭建了一个完整的全栈 Web 应用 — **SkillHub**，一个面向 AI Agent 开发者的技能共享平台。

## 🤯 关于开发过程

整个项目使用 **Kiro**（AI IDE）搭配 **Claude Opus 4.6** 模型完成，包括：

- 数据库设计与 JPA 实体建模
- Spring Boot 后端 REST API 开发
- GitHub OAuth2 + JWT 认证体系
- Vue 3 前端 SPA 开发
- Docker 容器化部署方案
- 单元测试与属性测试

没有复制粘贴 StackOverflow，没有手动敲一行代码，全部由 AI 理解需求后生成。这就是 2026 年的开发体验。

开发过程主要通过 Kiro 的 **Spec**（需求 → 设计 → 任务）模式驱动，结构化地完成每个功能模块。

## 🎯 SkillHub 解决什么问题？

开发 AI Agent 时，很多能力（文件操作、API 调用、数据处理等）都在重复造轮子。SkillHub 让这些技能像 npm 包一样可以被发现、下载和复用。

## ✨ 核心亮点

- **一键导入** — GitHub OAuth 登录，导入仓库，自动扫描所有 Agent Skills
- **精准下载** — 基于 git sparse-checkout，只拉取需要的 Skill 文件夹，不浪费带宽
- **全文搜索** — PostgreSQL 全文索引，快速找到你需要的技能
- **零依赖** — 下载命令只需要 git，跨平台通用
- **自动同步** — GitHub Webhook + 定时任务，仓库更新自动同步

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 · Spring Boot 3.5 · Spring Security · Spring Data JPA |
| 数据库 | PostgreSQL（测试使用 H2） |
| 认证 | GitHub OAuth2 + JWT |
| 缓存 | Caffeine |
| 前端 | Vue 3.5 · TypeScript · Vite 6 · Tailwind CSS |
| 测试 | JUnit 5 · jqwik · Vitest · @vue/test-utils |
| 部署 | Docker Compose · Nginx 反向代理 |

## 🚀 使用场景

1. 你开发了一个好用的 Agent Skill，想分享给社区 → 导入仓库，自动发布
2. 你需要某个功能的 Skill → 搜索、浏览、一键复制下载命令
3. 你想批量获取某个作者的所有 Skills → Skill Group 批量下载

## 🔗 链接

- GitHub: https://github.com/ghdefe/skill-hub
- 在线体验: https://skillhub.defe.eu.org

欢迎试用、Star 和反馈！如果你有好的 Agent Skills，也欢迎导入到平台分享给大家 🎉

#AI #Agent #OpenSource #Kiro #Claude #SpringBoot #Vue #全程AI生成
