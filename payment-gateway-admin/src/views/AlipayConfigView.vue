<template>
  <section>
    <el-card shadow="never">
      <el-form :model="form" label-position="top">
        <div class="form-grid">
          <el-form-item label="配置名称">
            <el-input v-model="form.configName" />
          </el-form-item>
          <el-form-item label="支付宝 AppId">
            <el-input v-model="form.appId" />
          </el-form-item>
          <el-form-item label="支付宝网关">
            <el-input v-model="form.gatewayUrl" />
          </el-form-item>
          <el-form-item label="签名类型">
            <el-input v-model="form.signType" disabled />
          </el-form-item>
          <el-form-item label="异步通知地址">
            <el-input v-model="form.notifyUrl" />
          </el-form-item>
          <el-form-item label="同步跳转地址">
            <el-input v-model="form.returnUrl" />
          </el-form-item>
        </div>
        <el-form-item label="应用私钥">
          <el-input v-model="form.appPrivateKey" type="textarea" :rows="6" placeholder="保存时会覆盖原私钥；留空表示不修改" />
        </el-form-item>
        <el-form-item label="支付宝公钥">
          <el-input v-model="form.alipayPublicKey" type="textarea" :rows="6" placeholder="保存时会覆盖原公钥；留空表示不修改" />
        </el-form-item>
        <div class="actions-row">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
          <el-switch v-model="form.sandbox" active-text="沙箱" inactive-text="正式" />
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
  appId: "",
  gatewayUrl: "https://openapi.alipay.com/gateway.do",
  appPrivateKey: "",
  alipayPublicKey: "",
  notifyUrl: "",
  returnUrl: "",
  signType: "RSA2",
  enabled: true,
  sandbox: false
});

onMounted(load);

async function load() {
  try {
    const data = await fetchAlipayConfig();
    Object.assign(form, {
      configName: data.config_name || "default",
      appId: data.app_id || "",
      gatewayUrl: data.gateway_url || "https://openapi.alipay.com/gateway.do",
      appPrivateKey: "",
      alipayPublicKey: "",
      notifyUrl: data.notify_url || "",
      returnUrl: data.return_url || "",
      signType: data.sign_type || "RSA2",
      enabled: Boolean(data.enabled),
      sandbox: Boolean(data.sandbox)
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
      appId: form.appId,
      gatewayUrl: form.gatewayUrl,
      appPrivateKey: form.appPrivateKey,
      alipayPublicKey: form.alipayPublicKey,
      notifyUrl: form.notifyUrl,
      returnUrl: form.returnUrl,
      signType: form.signType,
      enabled: form.enabled,
      sandbox: form.sandbox
    });
    form.appPrivateKey = "";
    form.alipayPublicKey = "";
    ElMessage.success("配置已保存");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "保存配置失败");
  } finally {
    saving.value = false;
  }
}
</script>
