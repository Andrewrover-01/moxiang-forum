<template>
  <el-footer class="app-footer" height="60px">
    <div class="footer-inner">
      <span>© 2024 墨香论坛 · 小说爱好者的家园</span>
      <span class="footer-links">
        <a href="#">关于我们</a>
        <a href="#">帮助中心</a>
        <a href="#">意见反馈</a>
      </span>
    </div>
  </el-footer>
</template>

<script setup lang="ts">
// AppFooter — static footer bar, no reactive state needed.
</script>

<style scoped>
.app-footer {
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #909399;
  font-size: 13px;
}

.footer-links {
  display: flex;
  gap: 16px;
}

.footer-links a {
  color: #909399;
  text-decoration: none;
}

.footer-links a:hover {
  color: #409eff;
}
</style>
