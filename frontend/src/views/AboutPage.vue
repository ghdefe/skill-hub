<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goToSkills() {
  router.push('/')
}
</script>

<template>
  <div class="max-w-3xl mx-auto">
    <!-- Hero -->
    <div class="text-center mb-12">
      <h1 class="text-4xl font-bold text-gray-900 mb-4">🔥 SkillHub</h1>
      <p class="text-lg text-gray-500 leading-relaxed">
        发现、发布、一键获取 AI Agent Skills<br />
        让智能体能力像积木一样自由组合
      </p>
    </div>

    <!-- Advantages -->
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-5 mb-12">
      <div class="bg-white rounded-lg border border-gray-200 p-5">
        <div class="text-2xl mb-2">🌍</div>
        <h3 class="text-base font-semibold text-gray-900 mb-1">跨平台，零依赖</h3>
        <p class="text-sm text-gray-500 leading-relaxed">
          下载命令仅依赖 git，无需安装额外工具。Windows、macOS、Linux 一行命令即可导入 Skill。
        </p>
      </div>

      <div class="bg-white rounded-lg border border-gray-200 p-5">
        <div class="text-2xl mb-2">🎈</div>
        <h3 class="text-base font-semibold text-gray-900 mb-1">轻量化，深度结合 GitHub</h3>
        <p class="text-sm text-gray-500 leading-relaxed">
          不重复造轮子，复用 GitHub 的版本管理、Star、Fork、Webhook 等基础设施，保持平台轻量。
        </p>
      </div>

      <div class="bg-white rounded-lg border border-gray-200 p-5">
        <div class="text-2xl mb-2">📐</div>
        <h3 class="text-base font-semibold text-gray-900 mb-1">标准化格式</h3>
        <p class="text-sm text-gray-500 leading-relaxed">
          按照标准化 Agent Skills 格式创建的仓库可一键导入，平台自动扫描发现所有 Skills。
        </p>
      </div>

      <div class="bg-white rounded-lg border border-gray-200 p-5">
        <div class="text-2xl mb-2">🎯</div>
        <h3 class="text-base font-semibold text-gray-900 mb-1">精准拉取</h3>
        <p class="text-sm text-gray-500 leading-relaxed">
          基于 git sparse-checkout，只下载需要的 Skill 文件夹，不浪费带宽和磁盘空间。
        </p>
      </div>
    </div>

    <!-- How it works -->
    <div class="mb-12">
      <h2 class="text-xl font-semibold text-gray-900 mb-4">如何使用</h2>
      <div class="space-y-4">
        <div class="flex gap-4 items-start">
          <span class="shrink-0 w-8 h-8 bg-gray-900 text-white rounded-full flex items-center justify-center text-sm font-medium">1</span>
          <div>
            <p class="text-sm font-medium text-gray-900">浏览和搜索</p>
            <p class="text-sm text-gray-500">在首页搜索或按标签筛选你需要的 Agent Skill</p>
          </div>
        </div>
        <div class="flex gap-4 items-start">
          <span class="shrink-0 w-8 h-8 bg-gray-900 text-white rounded-full flex items-center justify-center text-sm font-medium">2</span>
          <div>
            <p class="text-sm font-medium text-gray-900">复制命令</p>
            <p class="text-sm text-gray-500">在 Skill 详情页点击"复制"按钮获取下载命令</p>
          </div>
        </div>
        <div class="flex gap-4 items-start">
          <span class="shrink-0 w-8 h-8 bg-gray-900 text-white rounded-full flex items-center justify-center text-sm font-medium">3</span>
          <div>
            <p class="text-sm font-medium text-gray-900">终端执行</p>
            <p class="text-sm text-gray-500">在终端粘贴并运行，Skill 文件夹即刻到手</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Example command -->
    <div class="mb-12">
      <h2 class="text-xl font-semibold text-gray-900 mb-3">示例命令</h2>
      <div class="bg-gray-50 rounded-lg border border-gray-200 p-4">
        <code class="text-sm text-gray-800 font-mono break-all leading-relaxed">
          git clone --filter=blob:none --no-checkout --depth=1 https://github.com/user/repo
          && cd repo
          && git sparse-checkout init --no-cone
          && git sparse-checkout set skills/my-skill
          && git checkout
        </code>
      </div>
      <p class="text-xs text-gray-400 mt-2">只需要安装 git，无需其他依赖</p>
    </div>

    <!-- CTA -->
    <div class="text-center">
      <button
        class="px-6 py-2.5 bg-gray-900 text-white rounded-lg text-sm font-medium hover:bg-gray-800 transition-colors"
        @click="goToSkills"
      >
        开始探索 Skills →
      </button>
    </div>
  </div>
</template>
