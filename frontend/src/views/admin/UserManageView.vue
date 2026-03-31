<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header><span>用户管理</span></template>
      <router-view v-if="false" />
      <el-empty description="内容加载中..." />
    </el-card>
  </div>
</template>
<script setup lang="ts"></script>
<style scoped>.page-container{}</style>
