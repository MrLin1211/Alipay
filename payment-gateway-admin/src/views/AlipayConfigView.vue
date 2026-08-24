<template>
  <section>
    <el-card shadow="never">
      <el-form :model="form" label-position="top">
        <div class="form-grid">
          <el-form-item label="配置名称">
            <el-input v-model="form.configName" />
          </el-form-item>
          <el-form-item label="平台网关">
            <el-input v-model="form.host" />
          </el-form-item>
          <el-form-item label="平台商户ID">
            <el-input v-model="form.externalId" />
          </el-form-item>
          <el-form-item label="异步通知地址">
            <el-input v-model="form.notifyUrl" />
          </el-form-item>
        </div>
        <el-form-item label="MD5密钥">
          <el-input v-model="form.md5Key" type="password" show-password placeholder="保存时会覆盖原密钥；留空表示不修改" />
        </el-form-item>
        <el-form-item label="AES密钥">
          <el-input v-model="form.aesKey" type="password" show-password placeholder="保存时会覆盖原密钥；留空表示不修改" />
        </el-form-item>
        <div class="actions-row">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
          <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </el-form>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { fetchAlipayConfig, saveAlipayConfig } from "../api/gatewayAdminApi";

const saving = ref(false);
const form = reactive({
  configName: "default",
  host: "https://pay.zhenbaoge.com",
  externalId: "",
  md5Key: "",
  aesKey: "",
  notifyUrl: "",
  enabled: true
});

onMounted(load);

async function load() {
  try {
    const data = await fetchAlipayConfig();
    Object.assign(form, {
      configName: data.config_name || "default",
      host: data.host || "https://pay.zhenbaoge.com",
      externalId: data.external_id || "",
      md5Key: "",
      aesKey: "",
      notifyUrl: data.notify_url || "",
      enabled: Boolean(data.enabled)
    });
  } catch (error) {
    ElMessage.error(error.message || "加载配置失败");
  }
}

async function save() {
  saving.value = true;
  try {
    await saveAlipayConfig({
      configName: form.configName,
      host: form.host,
      externalId: form.externalId,
      md5Key: form.md5Key,
      aesKey: form.aesKey,
      notifyUrl: form.notifyUrl,
      enabled: form.enabled
    });
    form.md5Key = "";
    form.aesKey = "";
    ElMessage.success("配置已保存");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "保存配置失败");
  } finally {
    saving.value = false;
  }
}
</script>
