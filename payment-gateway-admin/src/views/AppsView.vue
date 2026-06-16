<template>
  <section>
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="AppId">
          <el-input v-model="filters.appId" clearable placeholder="按 AppId 查询" />
        </el-form-item>
        <el-form-item label="应用名称">
          <el-input v-model="filters.appName" clearable placeholder="按名称查询" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.enabled" clearable placeholder="全部状态" class="small-select">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增应用</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="apps" border>
        <el-table-column prop="app_id" label="AppId" min-width="150" />
        <el-table-column prop="app_name" label="应用名称" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? "启用" : "停用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="notify_url" label="业务通知地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip_whitelist" label="IP白名单" min-width="160" show-overflow-tooltip />
        <el-table-column prop="created_at" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增接入应用' : '编辑接入应用'" :width="dialogWidth">
      <el-form :model="form" label-position="top">
        <el-form-item v-if="dialogMode === 'edit'" label="AppId">
          <el-input v-model="form.appId" disabled />
        </el-form-item>
        <el-form-item label="应用名称" required>
          <el-input v-model="form.appName" />
        </el-form-item>
        <el-form-item label="业务通知地址">
          <el-input v-model="form.notifyUrl" />
        </el-form-item>
        <el-form-item label="IP白名单">
          <el-input v-model="form.ipWhitelist" placeholder="多个 IP 用英文逗号分隔" />
        </el-form-item>
        <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">{{ dialogMode === 'create' ? '创建' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="credentialVisible" title="应用密钥已生成" :width="dialogWidth">
      <el-alert
        title="AppSecret 只在本次创建后展示，请保存到业务系统环境变量或配置中心。"
        type="warning"
        show-icon
        :closable="false"
      />
      <el-descriptions :column="1" border class="credential-box">
        <el-descriptions-item label="应用名称">{{ createdCredential.appName }}</el-descriptions-item>
        <el-descriptions-item label="AppId">
          <span class="mono">{{ createdCredential.appId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="AppSecret">
          <span class="mono">{{ createdCredential.appSecret }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="copyCredential">复制</el-button>
        <el-button type="primary" @click="credentialVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Plus, RefreshLeft, Search } from "@element-plus/icons-vue";
import { createApp, fetchApps, updateApp } from "../api/gatewayAdminApi";

const loading = ref(false);
const saving = ref(false);
const apps = ref([]);
const dialogVisible = ref(false);
const credentialVisible = ref(false);
const dialogMode = ref("create");
const viewportWidth = ref(window.innerWidth);
const dialogWidth = computed(() => (viewportWidth.value <= 760 ? "calc(100% - 24px)" : "560px"));
const filters = reactive({
  appId: "",
  appName: "",
  enabled: ""
});
const form = reactive({
  id: null,
  appId: "",
  appName: "",
  notifyUrl: "",
  ipWhitelist: "",
  enabled: true
});
const createdCredential = reactive({
  appName: "",
  appId: "",
  appSecret: ""
});

onMounted(() => {
  window.addEventListener("resize", updateViewportWidth);
  load();
});
onUnmounted(() => window.removeEventListener("resize", updateViewportWidth));

function updateViewportWidth() {
  viewportWidth.value = window.innerWidth;
}

function openCreate() {
  dialogMode.value = "create";
  Object.assign(form, {
    id: null,
    appId: "",
    appName: "",
    notifyUrl: "",
    ipWhitelist: "",
    enabled: true
  });
  dialogVisible.value = true;
}

function openEdit(row) {
  dialogMode.value = "edit";
  Object.assign(form, {
    id: row.id,
    appId: row.app_id,
    appName: row.app_name,
    notifyUrl: row.notify_url || "",
    ipWhitelist: row.ip_whitelist || "",
    enabled: Boolean(row.enabled)
  });
  dialogVisible.value = true;
}

async function load() {
  loading.value = true;
  try {
    apps.value = await fetchApps({
      appId: filters.appId || undefined,
      appName: filters.appName || undefined,
      enabled: filters.enabled === "" ? undefined : filters.enabled
    });
  } catch (error) {
    ElMessage.error(error.message || "加载应用失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  load();
}

function resetFilters() {
  Object.assign(filters, {
    appId: "",
    appName: "",
    enabled: ""
  });
  load();
}

async function submit() {
  if (!form.appName) {
    ElMessage.warning("请填写应用名称");
    return;
  }
  saving.value = true;
  try {
    if (dialogMode.value === "create") {
      const data = await createApp(form);
      Object.assign(createdCredential, data);
      ElMessage.success("应用已创建");
      credentialVisible.value = true;
    } else {
      await updateApp(form.id, form);
      ElMessage.success("应用已更新");
    }
    dialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error.message || "保存应用失败");
  } finally {
    saving.value = false;
  }
}

async function copyCredential() {
  const text = `AppId=${createdCredential.appId}\nAppSecret=${createdCredential.appSecret}`;
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success("已复制");
  } catch {
    ElMessage.error("复制失败，请手动选择复制");
  }
}
</script>
