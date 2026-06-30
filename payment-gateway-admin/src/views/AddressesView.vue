<template>
  <section>
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="用户手机号">
          <el-input v-model="filters.phone" clearable maxlength="11" placeholder="商城账号手机号" />
        </el-form-item>
        <el-form-item label="收货人">
          <el-input v-model="filters.receiverName" clearable placeholder="收货人姓名" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="filters.keyword" clearable placeholder="电话 / 地区 / 详细地址" style="width:220px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="创建时间">
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
      <el-table v-loading="loading" :data="addresses" border>
        <el-table-column prop="userPhone" label="用户手机号" min-width="130" />
        <el-table-column prop="userDisplayName" label="用户昵称" min-width="120">
          <template #default="{ row }">{{ row.userDisplayName || "-" }}</template>
        </el-table-column>
        <el-table-column label="收货人" min-width="160">
          <template #default="{ row }">
            <div class="address-person">
              <strong>{{ row.receiverName }}</strong>
              <span>{{ row.phone }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="收货地址" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">{{ fullAddress(row) }}</template>
        </el-table-column>
        <el-table-column label="默认" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success">默认</el-tag>
            <span v-else class="muted-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" :disabled="row.isDefault" @click="makeDefault(row)">设为默认</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" title="编辑收货地址" width="620px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="商城用户">
          <el-input :model-value="`${form.userPhone || '-'} / ${form.userDisplayName || '-'}`" disabled />
        </el-form-item>
        <el-form-item label="收货人" required>
          <el-input v-model="form.receiverName" maxlength="32" />
        </el-form-item>
        <el-form-item label="收货电话" required>
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="省份" required>
          <el-input v-model="form.province" maxlength="32" />
        </el-form-item>
        <el-form-item label="城市" required>
          <el-input v-model="form.city" maxlength="32" />
        </el-form-item>
        <el-form-item label="区县" required>
          <el-input v-model="form.district" maxlength="32" />
        </el-form-item>
        <el-form-item label="详细地址" required>
          <el-input v-model="form.detailAddress" maxlength="256" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { RefreshLeft, Search } from "@element-plus/icons-vue";
import { deleteAddress, fetchAddresses, setDefaultAddress, updateAddress } from "../api/gatewayAdminApi";

const filters = reactive({ phone: "", receiverName: "", keyword: "", createdAtRange: [] });
const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
];
const page = reactive({ current: 0, size: 20, total: 0 });
const addresses = ref([]);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const form = reactive({
  id: null,
  userPhone: "",
  userDisplayName: "",
  receiverName: "",
  phone: "",
  province: "",
  city: "",
  district: "",
  detailAddress: ""
});

onMounted(load);

async function load() {
  loading.value = true;
  try {
    const data = await fetchAddresses({
      phone: filters.phone || undefined,
      receiverName: filters.receiverName || undefined,
      keyword: filters.keyword || undefined,
      createdAtStart: filters.createdAtRange?.[0] || undefined,
      createdAtEnd: filters.createdAtRange?.[1] || undefined,
      page: page.current,
      size: page.size
    });
    addresses.value = data.content || [];
    page.total = Number(data.totalElements || 0);
  } catch (error) {
    ElMessage.error(error.message || "加载收货地址失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  page.current = 0;
  load();
}

function reset() {
  Object.assign(filters, { phone: "", receiverName: "", keyword: "", createdAtRange: [] });
  search();
}

function handlePageChange(pageNo) {
  page.current = pageNo - 1;
  load();
}

function fullAddress(row) {
  return [row.province, row.city, row.district, row.detailAddress].filter(Boolean).join(" ") || "-";
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    userPhone: row.userPhone,
    userDisplayName: row.userDisplayName,
    receiverName: row.receiverName,
    phone: row.phone,
    province: row.province,
    city: row.city,
    district: row.district,
    detailAddress: row.detailAddress
  });
  dialogVisible.value = true;
}

async function save() {
  if (!form.receiverName.trim() || !form.phone.trim() || !form.province.trim() || !form.city.trim() || !form.district.trim() || !form.detailAddress.trim()) {
    ElMessage.warning("请完整填写收货地址");
    return;
  }
  saving.value = true;
  try {
    await updateAddress(form.id, {
      receiverName: form.receiverName,
      phone: form.phone,
      province: form.province,
      city: form.city,
      district: form.district,
      detailAddress: form.detailAddress
    });
    ElMessage.success("地址已更新");
    dialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

async function makeDefault(row) {
  try {
    await setDefaultAddress(row.id);
    ElMessage.success("默认地址已更新");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "操作失败");
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除 ${row.receiverName} 的收货地址吗？`, "删除确认", { type: "warning" });
    await deleteAddress(row.id);
    ElMessage.success("地址已删除");
    await load();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error.message || "删除失败");
    }
  }
}
</script>

<style scoped>
.address-person {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.address-person strong {
  color: #1f2937;
}

.address-person span,
.muted-text {
  color: #909399;
}
</style>
