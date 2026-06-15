<template>
  <div class="login-page">
    <el-card class="login-card" shadow="never">
      <h1>支付后台</h1>
      <p>请输入管理员账号和密码</p>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            placeholder="请输入密码"
            type="password"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>
        <el-form-item label="后端地址">
          <el-input v-model="backendUrl" @change="applyBackendUrl" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="login-button" @click="submit">
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { backendBaseUrl, saveAdminSession, setBackendBaseUrl } from "../api/client";
import { loginAdmin } from "../api/adminApi";

const emit = defineEmits(["logged-in"]);

const loading = ref(false);
const backendUrl = ref(backendBaseUrl.value);
const form = reactive({
  username: "admin",
  password: ""
});

function applyBackendUrl() {
  setBackendBaseUrl(backendUrl.value);
  backendUrl.value = backendBaseUrl.value;
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning("请输入账号和密码");
    return;
  }
  loading.value = true;
  try {
    const session = await loginAdmin(form);
    saveAdminSession(session);
    emit("logged-in", session.user);
    ElMessage.success("登录成功");
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "登录失败");
  } finally {
    loading.value = false;
  }
}
</script>
