<template>
  <section>
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="商家编号">
          <el-input v-model="filters.merchantNo" clearable />
        </el-form-item>
        <el-form-item label="商家名称">
          <el-input v-model="filters.merchantName" clearable />
        </el-form-item>
        <el-form-item label="登录账号">
          <el-input v-model="filters.username" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" class="small-select">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker
            v-model="filters.createdAtRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :default-time="defaultTime"
            class="date-range"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="merchants" border>
        <el-table-column prop="merchant_no" label="商家编号" min-width="160" />
        <el-table-column prop="merchant_name" label="商家名称" min-width="170" />
        <el-table-column prop="username" label="登录账号" min-width="130" />
        <el-table-column prop="contact_name" label="联系人" width="120" />
        <el-table-column prop="contact_phone" label="联系电话" width="140" />
        <el-table-column label="商家状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === "ACTIVE" ? "启用" : "停用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.user_enabled ? 'success' : 'info'">{{ row.user_enabled ? "启用" : "停用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接入应用" min-width="190">
          <template #default="{ row }">
            <div>{{ row.app_name || "-" }}</div>
            <small class="muted">{{ row.app_id || row.merchant_no }}</small>
          </template>
        </el-table-column>
        <el-table-column label="应用状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.app_enabled ? 'success' : 'info'">{{ row.app_enabled ? "启用" : "停用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="注册时间" min-width="170" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openMerchant(row)">商家</el-button>
            <el-button link type="primary" @click="openApp(row)">应用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          layout="total, prev, pager, next"
          :total="page.total"
          :page-size="page.size"
          :current-page="page.current + 1"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="merchantDialogVisible" title="编辑商家" width="620px">
      <el-form :model="merchantForm" label-position="top">
        <el-form-item label="商家编号">
          <el-input v-model="merchantForm.merchantNo" disabled />
        </el-form-item>
        <el-form-item label="商家名称" required>
          <el-input v-model="merchantForm.merchantName" />
        </el-form-item>
        <div class="form-grid two-columns">
          <el-form-item label="联系人">
            <el-input v-model="merchantForm.contactName" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="merchantForm.contactPhone" />
          </el-form-item>
        </div>
        <div class="form-grid two-columns">
          <el-form-item label="商家状态">
            <el-select v-model="merchantForm.status" class="full-control">
              <el-option label="启用" value="ACTIVE" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item label="登录账号">
            <el-switch v-model="merchantForm.userEnabled" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="merchantDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMerchant">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appDialogVisible" title="接入应用配置" width="620px">
      <el-form :model="appForm" label-position="top">
        <el-form-item label="AppId">
          <el-input v-model="appForm.appId" disabled />
        </el-form-item>
        <el-form-item label="应用名称">
          <el-input v-model="appForm.appName" />
        </el-form-item>
        <el-form-item label="业务通知地址">
          <el-input v-model="appForm.notifyUrl" />
        </el-form-item>
        <el-form-item label="IP白名单">
          <el-input v-model="appForm.ipWhitelist" placeholder="多个 IP 用英文逗号分隔" />
        </el-form-item>
        <el-switch v-model="appForm.enabled" active-text="启用" inactive-text="停用" />
      </el-form>
      <template #footer>
        <el-button @click="appDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveApp">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { RefreshLeft, Search } from "@element-plus/icons-vue";
import { fetchMerchants, updateMerchant, updateMerchantApp } from "../api/gatewayAdminApi";

const filters = reactive({
  merchantNo: "",
  merchantName: "",
  username: "",
  status: "",
  createdAtRange: []
});
const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
];
const page = reactive({ current: 0, size: 20, total: 0 });
const merchants = ref([]);
const loading = ref(false);
const saving = ref(false);
const merchantDialogVisible = ref(false);
const appDialogVisible = ref(false);
const merchantForm = reactive(defaultMerchantForm());
const appForm = reactive(defaultAppForm());

onMounted(load);

function defaultMerchantForm() {
  return {
    id: null,
    merchantNo: "",
    merchantName: "",
    contactName: "",
    contactPhone: "",
    status: "ACTIVE",
    userEnabled: true
  };
}

function defaultAppForm() {
  return {
    id: null,
    appId: "",
    appName: "",
    notifyUrl: "",
    ipWhitelist: "",
    enabled: true
  };
}

async function load() {
  loading.value = true;
  try {
    const data = await fetchMerchants({
      merchantNo: filters.merchantNo || undefined,
      merchantName: filters.merchantName || undefined,
      username: filters.username || undefined,
      status: filters.status || undefined,
      createdAtStart: filters.createdAtRange?.[0] || undefined,
      createdAtEnd: filters.createdAtRange?.[1] || undefined,
      page: page.current,
      size: page.size
    });
    merchants.value = data.content || [];
    page.total = Number(data.totalElements || 0);
  } catch (error) {
    ElMessage.error(error.message || "加载商家失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  page.current = 0;
  load();
}

function reset() {
  Object.assign(filters, {
    merchantNo: "",
    merchantName: "",
    username: "",
    status: "",
    createdAtRange: []
  });
  search();
}

function handlePageChange(pageNo) {
  page.current = pageNo - 1;
  load();
}

function openMerchant(row) {
  Object.assign(merchantForm, {
    id: row.id,
    merchantNo: row.merchant_no,
    merchantName: row.merchant_name,
    contactName: row.contact_name || "",
    contactPhone: row.contact_phone || "",
    status: row.status || "ACTIVE",
    userEnabled: Boolean(row.user_enabled)
  });
  merchantDialogVisible.value = true;
}

function openApp(row) {
  Object.assign(appForm, {
    id: row.id,
    appId: row.app_id || row.merchant_no,
    appName: row.app_name || row.merchant_name,
    notifyUrl: row.notify_url || "",
    ipWhitelist: row.ip_whitelist || "",
    enabled: row.app_enabled === undefined ? true : Boolean(row.app_enabled)
  });
  appDialogVisible.value = true;
}

async function saveMerchant() {
  if (!merchantForm.merchantName.trim()) {
    ElMessage.warning("请填写商家名称");
    return;
  }
  saving.value = true;
  try {
    await updateMerchant(merchantForm.id, {
      merchantName: merchantForm.merchantName,
      contactName: merchantForm.contactName,
      contactPhone: merchantForm.contactPhone,
      status: merchantForm.status,
      userEnabled: merchantForm.userEnabled
    });
    ElMessage.success("商家已更新");
    merchantDialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error.message || "保存商家失败");
  } finally {
    saving.value = false;
  }
}

async function saveApp() {
  saving.value = true;
  try {
    await updateMerchantApp(appForm.id, {
      appName: appForm.appName,
      notifyUrl: appForm.notifyUrl,
      ipWhitelist: appForm.ipWhitelist,
      enabled: appForm.enabled
    });
    ElMessage.success("接入应用已更新");
    appDialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error.message || "保存接入应用失败");
  } finally {
    saving.value = false;
  }
}
</script>
