<template>
  <el-card shadow="never">
    <el-alert
      title="这里控制当前商家的支付通道。商城发起支付后会先读取当前商家配置，再调用管理后台支付网关，由管理后台继续调用支付宝。密钥留空或保持掩码表示不修改。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-form v-loading="loading" class="config-form" :model="config" label-position="top">
      <el-form-item label="支付通道">
        <el-radio-group v-model="config.payChannel">
          <el-radio-button label="MALLHOME">Mallhome</el-radio-button>
          <el-radio-button label="PAYMENT_GATEWAY">自建支付网关</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-divider content-position="left">Mallhome 配置</el-divider>
      <div class="config-grid">
        <el-form-item label="host">
          <el-input v-model="config.host" />
        </el-form-item>
        <el-form-item label="externalId">
          <el-input v-model="config.externalId" />
        </el-form-item>
        <el-form-item label="notifyUrl">
          <el-input v-model="config.notifyUrl" />
        </el-form-item>
        <el-form-item label="returnUrl">
          <el-input v-model="config.returnUrl" />
        </el-form-item>
        <el-form-item label="默认支付方式">
          <el-select v-model="config.defaultPayMethodType">
            <el-option label="支付宝" value="ALIPAY_CN" />
            <el-option label="国际支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHATPAY" />
            <el-option label="银行卡" value="CARD" />
          </el-select>
        </el-form-item>
      </div>

      <el-divider content-position="left">管理后台支付网关配置</el-divider>
      <div class="config-grid">
        <el-form-item label="gatewayHost">
          <el-input v-model="config.gatewayHost" placeholder="http://127.0.0.1:8090" />
        </el-form-item>
        <el-form-item label="gatewayAppId / 商家编号">
          <el-input v-model="config.gatewayAppId" />
        </el-form-item>
        <el-form-item label="gatewayAppSecret">
          <el-input v-model="config.gatewayAppSecret" show-password placeholder="留空或保持掩码表示不修改" />
        </el-form-item>
        <el-form-item label="gatewayReturnUrl">
          <el-input v-model="config.gatewayReturnUrl" />
        </el-form-item>
        <el-form-item label="gatewayBusinessNotifyUrl">
          <el-input v-model="config.gatewayBusinessNotifyUrl" />
        </el-form-item>
      </div>

      <div class="actions-row">
        <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
        <el-button @click="loadConfig">刷新</el-button>
      </div>
    </el-form>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { fetchPayConfig, savePayConfig } from "../api/adminApi";

const loading = ref(false);
const saving = ref(false);
const config = reactive({
  payChannel: "MALLHOME",
  host: "",
  externalId: "",
  notifyUrl: "",
  returnUrl: "",
  defaultPayMethodType: "ALIPAY_CN",
  gatewayHost: "",
  gatewayAppId: "",
  gatewayAppSecret: "",
  gatewayReturnUrl: "",
  gatewayBusinessNotifyUrl: ""
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

async function save() {
  saving.value = true;
  try {
    await savePayConfig(config);
    ElMessage.success("支付配置已保存");
    await loadConfig();
  } catch (error) {
    ElMessage.error(error.message || "保存支付配置失败");
  } finally {
    saving.value = false;
  }
}

onMounted(loadConfig);
</script>
