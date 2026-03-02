# 🔥 SkillHub — AI Agent 技能共享平台

> 发现、发布、一键获取 AI Agent Skills，让智能体能力像积木一样自由组合。

SkillHub 是一个面向 AI Agent 开发者的技能共享平台。用户通过 GitHub 登录后，可以导入自己的 GitHub 仓库，平台会自动扫描并发现其中的 Agent Skills，供社区搜索、浏览和下载。

## 💡 为什么选择 SkillHub

- **跨平台，零依赖** — 下载命令仅依赖 git，无需安装额外工具，任何操作系统一行命令即可导入 Skill
- **轻量化，深度结合 GitHub** — 不重复造轮子，复用 GitHub 的版本管理、Star、Fork、Webhook 等基础设施
- **标准化** — 按照标准化 Agent Skills 格式创建的仓库可一键导入，自动发现所有 Skills
- **精准拉取** — 基于 git sparse-checkout，只下载需要的 Skill 文件夹，不浪费带宽

## ✨ 核心特性

- **GitHub OAuth2 登录** — 一键授权，无需注册
- **仓库导入与自动扫描** — 导入 GitHub 仓库后自动发现 Skills
- **全文搜索** — 基于 PostgreSQL tsvector/GIN 索引的高性能搜索
- **标签过滤与排序** — 按标签、Star 数、下载量等多维度筛选
- **一键复制下载命令** — 生成 git sparse-checkout 命令，只拉取需要的 Skill 文件夹
- **批量下载** — Skill Group 级别的批量 sparse-checkout 命令
- **Webhook 同步** — GitHub Webhook 自动同步仓库变更
- **定时后台同步** — 定期更新仓库元数据（Star、Fork 等）

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 · Spring Boot 3.5 · Spring Security · Spring Data JPA |
| 数据库 | PostgreSQL（测试使用 H2） |
| 认证 | GitHub OAuth2 + JWT |
| 缓存 | Caffeine |
| 前端 | Vue 3.5 · TypeScript · Vite 6 · Tailwind CSS |
| 测试 | JUnit 5 · jqwik · Vitest · @vue/test-utils |

## 📁 项目结构

```
├── backend/                  # Spring Boot 后端
│   └── src/main/java/com/agentskills/sharing/
│       ├── controller/       # REST API (/api/*)
│       ├── service/          # 业务逻辑
│       ├── repository/       # 数据访问层
│       ├── entity/           # JPA 实体
│       ├── dto/              # 请求/响应 DTO
│       ├── security/         # OAuth2 + JWT 安全配置
│       └── config/           # Spring 配置
├── frontend/                 # Vue 3 SPA
│   └── src/
│       ├── views/            # 页面组件
│       ├── composables/      # Vue 组合式函数
│       ├── api/              # HTTP 客户端
│       ├── utils/            # 工具函数
│       └── router/           # 路由配置
```

## 🚀 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- PostgreSQL 15+（开发环境）

### 后端

```bash
cd backend

# 配置环境变量（或修改 application.yml）
export DATABASE_URL=jdbc:postgresql://localhost:5432/skills_sharing
export GITHUB_CLIENT_ID=your_client_id
export GITHUB_CLIENT_SECRET=your_client_secret
export JWT_SECRET=your-jwt-secret-at-least-256-bits

# 启动
./mvnw spring-boot:run
```

后端默认运行在 `http://localhost:18123`。

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器运行在 `http://localhost:5173`，自动代理 `/api` 请求到后端。

### 运行测试

```bash
# 后端测试
cd backend && ./mvnw test

# 前端测试
cd frontend && npm run test
```

## 📦 Skill 下载方式

平台为每个 Skill 生成 git sparse-checkout 命令，用户只需复制并执行即可精准拉取目标文件夹：

```bash
git clone --filter=blob:none --no-checkout --depth=1 https://github.com/user/repo .skillhub-tmp-repo \
  && cd .skillhub-tmp-repo \
  && git sparse-checkout init --no-cone \
  && git sparse-checkout set skills/my-skill \
  && git checkout \
  && cp -r skills/my-skill ../my-skill \
  && cd .. && rm -rf .skillhub-tmp-repo
```

也支持批量下载整个 Skill Group 中的所有 Skills。

## 🔑 核心概念

| 概念 | 说明 |
|------|------|
| **Skill** | 单个 Agent 技能，对应仓库中的一个文件夹 |
| **Skill Group** | 来自同一仓库的技能集合，与 Repository 一一对应 |
| **Repository** | 用户导入的 GitHub 仓库，跟踪 Star/Fork/同步状态 |
| **Tag** | 技能标签，用于分类和筛选 |

## 📄 License

MIT
