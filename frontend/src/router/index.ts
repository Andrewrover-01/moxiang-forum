import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/forum',
      name: 'forum-list',
      component: () => import('@/views/forum/ForumListView.vue')
    },
    {
      path: '/forum/:id',
      name: 'forum-detail',
      component: () => import('@/views/forum/ForumDetailView.vue')
    },
    {
      path: '/post',
      name: 'post-list',
      component: () => import('@/views/post/PostListView.vue')
    },
    {
      path: '/post/create',
      name: 'post-create',
      component: () => import('@/views/post/PostCreateView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/post/:id',
      name: 'post-detail',
      component: () => import('@/views/post/PostDetailView.vue')
    },
    {
      path: '/novel',
      name: 'novel-list',
      component: () => import('@/views/novel/NovelListView.vue')
    },
    {
      path: '/novel/create',
      name: 'novel-create',
      component: () => import('@/views/novel/NovelCreateView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/novel/:id',
      name: 'novel-detail',
      component: () => import('@/views/novel/NovelDetailView.vue')
    },
    {
      path: '/novel/:novelId/chapter/:chapterId',
      name: 'chapter',
      component: () => import('@/views/novel/ChapterView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/user/LoginView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/user/RegisterView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/user/:id',
      name: 'profile',
      component: () => import('@/views/user/ProfileView.vue')
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/user/SettingsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue')
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/UserManageView.vue')
        },
        {
          path: 'posts',
          name: 'admin-posts',
          component: () => import('@/views/admin/PostManageView.vue')
        },
        {
          path: 'forums',
          name: 'admin-forums',
          component: () => import('@/views/admin/ForumManageView.vue')
        },
        {
          path: 'novels',
          name: 'admin-novels',
          component: () => import('@/views/admin/NovelManageView.vue')
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue')
    }
  ],
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

// Navigation guards
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return { name: 'home' }
  }
  if (to.meta.guestOnly && userStore.isLoggedIn) {
    return { name: 'home' }
  }
})

export default router
