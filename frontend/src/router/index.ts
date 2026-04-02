import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue')
  },
  {
    path: '/posts',
    name: 'Posts',
    component: () => import('@/views/PostsView.vue')
  },
  {
    path: '/forums',
    name: 'Forums',
    component: () => import('@/views/ForumsView.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { guest: true }
  },
  {
    path: '/forum/:id',
    name: 'Forum',
    component: () => import('@/views/ForumView.vue')
  },
  {
    path: '/post/:id',
    name: 'Post',
    component: () => import('@/views/PostView.vue')
  },
  {
    path: '/post/create',
    name: 'CreatePost',
    component: () => import('@/views/CreatePostView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/** 路由守卫：未登录时禁止访问需要认证的路由 */
router.beforeEach((to, _from) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  // 已登录时不允许进入登录/注册页
  if (to.meta.guest && userStore.isLoggedIn) {
    return { name: 'Home' }
  }
})

export default router
