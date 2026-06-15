<template>
  <el-card shadow="never">
    <el-alert title="配置来自后端环境变量，本页面只读展示，避免误改生产支付参数。" type="info" show-icon :closable="false" />
    <el-descriptions v-loading="loading" class="config-desc" :column="1" border>
      <el-descriptions-item label="host">{{ config.host || "-" }}</el-descriptions-item>
      <el-descriptions-item label="externalId">{{ config.externalId || "-" }}</el-descriptions-item>
      <el-descriptions-item label="notifyUrl">{{ config.notifyUrl || "-" }}</el-descriptions-item>
      <el-descriptions-item label="returnUrl">{{ config.returnUrl || "-" }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { fetchPayConfig } from "../api/adminApi";

const loading = ref(false);
const config = reactive({
  host: "",
  externalId: "",
  notifyUrl: "",
  returnUrl: ""
});

async function loadConfig() {
  loading.value = true;
  try {
    Object.assign(config, await fetchPayConfig());
  } catch (error) {
    ElMessage.error(error.message || "加载支付配置失败");
  } finally {
    loading.value = false;
  }
}

onMounted(loadConfig);
</script>
