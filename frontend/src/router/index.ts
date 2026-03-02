import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/HomePage.vue'),
    },
    {
      path: '/skills/:id',
      name: 'SkillDetail',
      component: () => import('@/views/SkillDetailPage.vue'),
    },
    {
      path: '/skill-groups/:id',
      name: 'SkillGroupDetail',
      component: () => import('@/views/SkillGroupDetailPage.vue'),
    },
    {
      path: '/auth/callback',
      name: 'AuthCallback',
      component: () => import('@/views/AuthCallbackPage.vue'),
    },
    {
      path: '/repositories',
      name: 'Repositories',
      component: () => import('@/views/RepositoriesPage.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

export default router
