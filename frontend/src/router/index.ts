import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

/** Route-level page titles (displayed in <title> and breadcrumbs). */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    requiresAdmin?: boolean
    guestOnly?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { title: '首页' }
    },
    {
      path: '/forum',
      name: 'forum-list',
      component: () => import('@/views/forum/ForumListView.vue'),
      meta: { title: '全部板块' }
    },
    {
      path: '/forum/:id',
      name: 'forum-detail',
      component: () => import('@/views/forum/ForumDetailView.vue'),
      meta: { title: '板块详情' }
    },
    {
      path: '/post',
      name: 'post-list',
      component: () => import('@/views/post/PostListView.vue'),
      meta: { title: '帖子列表' }
    },
    {
      path: '/post/create',
      name: 'post-create',
      component: () => import('@/views/post/PostCreateView.vue'),
      meta: { title: '发布帖子', requiresAuth: true }
    },
    {
      path: '/post/:id',
      name: 'post-detail',
      component: () => import('@/views/post/PostDetailView.vue'),
      meta: { title: '帖子详情' }
    },
    {
      path: '/novel',
      name: 'novel-list',
      component: () => import('@/views/novel/NovelListView.vue'),
      meta: { title: '小说列表' }
    },
    {
      path: '/novel/create',
      name: 'novel-create',
      component: () => import('@/views/novel/NovelCreateView.vue'),
      meta: { title: '发布小说', requiresAuth: true }
    },
    {
      path: '/novel/:id',
      name: 'novel-detail',
      component: () => import('@/views/novel/NovelDetailView.vue'),
      meta: { title: '小说详情' }
    },
    {
      path: '/novel/:novelId/chapter/:chapterId',
      name: 'chapter',
      component: () => import('@/views/novel/ChapterView.vue'),
      meta: { title: '阅读章节' }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/user/LoginView.vue'),
      meta: { title: '登录', guestOnly: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/user/RegisterView.vue'),
      meta: { title: '注册', guestOnly: true }
    },
    {
      path: '/user/:id',
      name: 'profile',
      component: () => import('@/views/user/ProfileView.vue'),
      meta: { title: '个人主页' }
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/user/SettingsView.vue'),
      meta: { title: '账号设置', requiresAuth: true }
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { title: '管理后台', requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
          meta: { title: '数据统计' }
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/UserManageView.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'posts',
          name: 'admin-posts',
          component: () => import('@/views/admin/PostManageView.vue'),
          meta: { title: '帖子管理' }
        },
        {
          path: 'forums',
          name: 'admin-forums',
          component: () => import('@/views/admin/ForumManageView.vue'),
          meta: { title: '板块管理' }
        },
        {
          path: 'novels',
          name: 'admin-novels',
          component: () => import('@/views/admin/NovelManageView.vue'),
          meta: { title: '小说管理' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' }
    }
  ],
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

const APP_NAME = '墨香论坛'

// ── Navigation guards ─────────────────────────────────────────────────────────
let userInfoRestored = false

router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // On the very first navigation after a page load, if a token exists but
  // userInfo has not been loaded yet (e.g. page refresh), restore it.
  if (!userInfoRestored) {
    userInfoRestored = true
    if (userStore.token && !userStore.userInfo) {
      await userStore.fetchUserInfo()
    }
  }

  // Auth guards
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

// ── Update document title after each navigation ───────────────────────────────
router.afterEach((to) => {
  const pageTitle = to.meta.title
  document.title = pageTitle ? `${pageTitle} - ${APP_NAME}` : APP_NAME
})

export default router

