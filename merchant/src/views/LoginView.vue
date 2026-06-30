<template>
  <div class="login-page">
    <el-card class="login-card merchant-login-card" shadow="never">
      <h1>商家后台</h1>
      <p>商家独立注册登录，登录后只管理自己的商品</p>

      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-position="top" @keyup.enter="submitLogin">
            <el-form-item label="手机号">
              <el-input v-model="loginForm.username" placeholder="请输入11位手机号" autocomplete="username" maxlength="11" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="loginForm.password"
                placeholder="请输入密码"
                type="password"
                autocomplete="current-password"
                show-password
              />
            </el-form-item>
            <el-button type="primary" :loading="loading" class="login-button" @click="submitLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册店铺" name="register">
          <el-form :model="registerForm" label-position="top" @keyup.enter="submitRegister">
            <el-form-item label="店铺名称">
              <el-input v-model="registerForm.merchantName" placeholder="请输入店铺名称" maxlength="128" />
            </el-form-item>
            <el-form-item label="登录手机号">
              <el-input v-model="registerForm.username" placeholder="请输入11位手机号" autocomplete="username" maxlength="11" />
            </el-form-item>
            <el-form-item label="登录密码">
              <el-input
                v-model="registerForm.password"
                placeholder="至少6位密码"
                type="password"
                autocomplete="new-password"
                show-password
              />
            </el-form-item>
            <div class="form-grid">
              <el-form-item label="联系人">
                <el-input v-model="registerForm.contactName" maxlength="64" />
              </el-form-item>
              <el-form-item label="联系电话">
                <el-input v-model="registerForm.contactPhone" maxlength="32" />
              </el-form-item>
            </div>
            <el-button type="primary" :loading="loading" class="login-button" @click="submitRegister">
              注册并进入后台
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { saveMerchantSession } from "../api/client";
import { loginAdmin, registerMerchant } from "../api/adminApi";

const emit = defineEmits(["logged-in"]);

const mode = ref("login");
const loading = ref(false);
const loginForm = reactive({
  username: "",
  password: ""
});
const registerForm = reactive({
  merchantName: "",
  username: "",
  password: "",
  contactName: "",
  contactPhone: ""
});

async function submitLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning("请输入手机号和密码");
    return;
  }
  if (!isPhone(loginForm.username)) {
    ElMessage.warning("登录账号必须是11位手机号");
    return;
  }
  await submit(() => loginAdmin(loginForm), "登录成功");
}

async function submitRegister() {
  if (!registerForm.merchantName || !registerForm.username || !registerForm.password) {
    ElMessage.warning("请输入店铺名称、手机号和密码");
    return;
  }
  if (!isPhone(registerForm.username)) {
    ElMessage.warning("登录账号必须是11位手机号");
    return;
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning("密码至少6位");
    return;
  }
  await submit(() => registerMerchant(registerForm), "注册成功");
}

function isPhone(value) {
  return /^1\d{10}$/.test(value);
}

async function submit(action, successMessage) {
  loading.value = true;
  try {
    const session = await action();
    saveMerchantSession(session);
    emit("logged-in", session.user);
    ElMessage.success(successMessage);
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || "操作失败");
  } finally {
    loading.value = false;
  }
}
</script>
