<template>
  <div>
    <el-card class="toolbar-card" shadow="never">
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增账号</el-button>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="users" stripe>
        <el-table-column prop="username" label="账号" min-width="140" />
        <el-table-column prop="displayName" label="显示名称" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? "启用" : "禁用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最后登录" min-width="190">
          <template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="190">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" :type="row.enabled ? 'warning' : 'success'" @click="toggleEnabled(row)">
              {{ row.enabled ? "禁用" : "启用" }}
            </el-button>
            <el-button size="small" type="danger" @click="removeUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingUser ? '编辑账号' : '新增账号'" class="account-dialog" :width="dialogWidth">
      <el-form :model="form" label-width="90px">
        <el-form-item label="账号" required>
          <el-input v-model="form.username" :disabled="Boolean(editingUser)" placeholder="字母、数字、下划线" />
        </el-form-item>
        <el-form-item label="显示名称" required>
          <el-input v-model="form.displayName" placeholder="例如：运营管理员" />
        </el-form-item>
        <el-form-item :label="editingUser ? '新密码' : '密码'" :required="!editingUser">
          <el-input v-model="form.password" type="password" show-password placeholder="留空则不修改密码" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import dayjs from "dayjs";
import { createAdminUser, deleteAdminUser, fetchAdminUsers, updateAdminUser } from "../api/adminApi";

const loading = ref(false);
const saving = ref(false);
const users = ref([]);
const dialogVisible = ref(false);
const editingUser = ref(null);
const viewportWidth = ref(window.innerWidth);
const dialogWidth = computed(() => (viewportWidth.value <= 760 ? "calc(100% - 24px)" : "460px"));
const form = reactive({
  username: "",
  displayName: "",
  password: "",
  enabled: true
});

onMounted(() => {
  window.addEventListener("resize", updateViewportWidth);
  loadUsers();
});

onUnmounted(() => {
  window.removeEventListener("resize", updateViewportWidth);
});

function updateViewportWidth() {
  viewportWidth.value = window.innerWidth;
}

async function loadUsers() {
  loading.value = true;
  try {
    users.value = await fetchAdminUsers();
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "账号列表加载失败");
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  editingUser.value = null;
  Object.assign(form, {
    username: "",
    displayName: "",
    password: "",
    enabled: true
  });
  dialogVisible.value = true;
}

function openEditDialog(user) {
  editingUser.value = user;
  Object.assign(form, {
    username: user.username,
    displayName: user.displayName,
    password: "",
    enabled: user.enabled
  });
  dialogVisible.value = true;
}

async function saveUser() {
  if (!form.displayName || (!editingUser.value && (!form.username || !form.password))) {
    ElMessage.warning("请填写必填项");
    return;
  }

  saving.value = true;
  try {
    if (editingUser.value) {
      const payload = {
        displayName: form.displayName,
        enabled: form.enabled
      };
      if (form.password) {
        payload.password = form.password;
      }
      await updateAdminUser(editingUser.value.id, payload);
    } else {
      await createAdminUser({
        username: form.username,
        displayName: form.displayName,
        password: form.password,
        enabled: form.enabled
      });
    }
    dialogVisible.value = false;
    ElMessage.success("保存成功");
    await loadUsers();
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

async function toggleEnabled(user) {
  try {
    await updateAdminUser(user.id, { enabled: !user.enabled });
    ElMessage.success("状态已更新");
    await loadUsers();
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "状态更新失败");
  }
}

async function removeUser(user) {
  try {
    await ElMessageBox.confirm(`确定删除账号「${user.username}」吗？`, "删除账号", {
      type: "warning"
    });
    await deleteAdminUser(user.id);
    ElMessage.success("账号已删除");
    await loadUsers();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error.response?.data?.message || "删除失败");
    }
  }
}

function formatTime(value) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss") : "-";
}
</script>
