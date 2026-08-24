<template>
  <section>
    <el-card shadow="never">
      <el-form :model="form" label-position="top">
        <div class="form-grid">
          <el-form-item label="AppId">
            <el-input v-model="form.appId" disabled />
          </el-form-item>
          <el-form-item label="应用名称">
            <el-input v-model="form.appName" disabled />
          </el-form-item>
        </div>
        <el-form-item label="业务通知地址">
          <el-input v-model="form.notifyUrl" placeholder="支付网关通知你方业务系统的地址" />
        </el-form-item>
        <el-form-item label="IP白名单">
          <el-input v-model="form.ipWhitelist" placeholder="多个 IP 用英文逗号分隔；留空表示不限制" />
        </el-form-item>
        <div class="button-row">
          <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
          <el-button type="warning" :loading="resetting" @click="resetSecret">重置 AppSecret</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </el-form>
    </el-card>

    <el-dialog v-model="secretVisible" title="新的 AppSecret" :width="dialogWidth">
      <el-alert
        title="AppSecret 只在本次重置后展示，请立即保存到你的业务后端配置中。"
        type="warning"
        show-icon
        :closable="false"
      />
      <el-descriptions :column="1" border class="credential-box">
        <el-descriptions-item label="AppId">
          <span class="mono">{{ secret.appId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="AppSecret">
          <span class="mono">{{ secret.appSecret }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="copySecret">复制</el-button>
        <el-button type="primary" @click="secretVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { fetchAppConfig, resetAppSecret, saveAppConfig } from "../api/clientPayApi";

const saving = ref(false);
const resetting = ref(false);
const secretVisible = ref(false);
const viewportWidth = ref(window.innerWidth);
const dialogWidth = computed(() => (viewportWidth.value <= 760 ? "calc(100% - 24px)" : "560px"));
const form = reactive({
  appId: "",
  appName: "",
  notifyUrl: "",
  ipWhitelist: ""
});
const secret = reactive({
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

async function load() {
  try {
    const data = await fetchAppConfig();
    Object.assign(form, {
      appId: data.appId || "",
      appName: data.appName || "",
      notifyUrl: data.notifyUrl || "",
      ipWhitelist: data.ipWhitelist || ""
    });
  } catch (error) {
    ElMessage.error(error.message || "加载接入配置失败");
  }
}

async function save() {
  saving.value = true;
  try {
    await saveAppConfig({
      notifyUrl: form.notifyUrl,
      ipWhitelist: form.ipWhitelist
    });
    ElMessage.success("配置已保存");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

async function resetSecret() {
  try {
    await ElMessageBox.confirm("重置后旧 AppSecret 会立即失效，确认继续？", "确认重置", {
      type: "warning",
      confirmButtonText: "确认重置",
      cancelButtonText: "取消"
    });
  } catch {
    return;
  }
  resetting.value = true;
  try {
    const data = await resetAppSecret();
    Object.assign(secret, data);
    secretVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || "重置失败");
  } finally {
    resetting.value = false;
  }
}

async function copySecret() {
  const text = `AppId=${secret.appId}\nAppSecret=${secret.appSecret}`;
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success("已复制");
  } catch {
    ElMessage.error("复制失败，请手动选择复制");
  }
}
</script>
