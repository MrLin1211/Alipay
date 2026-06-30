<template>
  <main class="login-page">
    <el-card class="login-card" shadow="never">
      <h1>管理后台</h1>
      <p>登录后管理支付宝渠道配置和接入应用</p>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button class="login-button" type="primary" native-type="submit" :loading="loading">登录</el-button>
      </el-form>
    </el-card>
  </main>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { loginAdmin } from "../api/gatewayAdminApi";

const emit = defineEmits(["logged-in"]);
const loading = ref(false);
const form = reactive({
  username: "admin",
  password: ""
});

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning("请输入账号和密码");
    return;
  }
  loading.value = true;
  try {
    const user = await loginAdmin(form);
    form.password = "";
    emit("logged-in", user);
  } catch (error) {
    ElMessage.error(error.message || "登录失败");
  } finally {
    loading.value = false;
  }
}
</script>
