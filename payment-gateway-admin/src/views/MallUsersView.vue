<template>
  <section>
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="手机号">
          <el-input v-model="filters.phone" clearable maxlength="11" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="filters.displayName" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.enabled" clearable placeholder="全部状态" class="status-select">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
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
      <el-table v-loading="loading" :data="users" border>
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="display_name" label="昵称" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? "启用" : "停用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="last_login_at" label="最后登录" min-width="170">
          <template #default="{ row }">{{ row.last_login_at || "-" }}</template>
        </el-table-column>
        <el-table-column prop="created_at" label="注册时间" min-width="170" />
        <el-table-column prop="updated_at" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" title="编辑用户" width="520px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="手机号">
          <el-input v-model="form.phone" disabled />
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input v-model="form.displayName" maxlength="64" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
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
import { ElMessage } from "element-plus";
import { RefreshLeft, Search } from "@element-plus/icons-vue";
import { fetchMallUsers, updateMallUser } from "../api/gatewayAdminApi";

const filters = reactive({ phone: "", displayName: "", enabled: "", createdAtRange: [] });
const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
];
const form = reactive({ id: null, phone: "", displayName: "", enabled: true });
const page = reactive({ current: 0, size: 20, total: 0 });
const users = ref([]);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);

onMounted(load);

async function load() {
  loading.value = true;
  try {
    const data = await fetchMallUsers({
      phone: filters.phone || undefined,
      displayName: filters.displayName || undefined,
      enabled: filters.enabled === "" ? undefined : filters.enabled,
      createdAtStart: filters.createdAtRange?.[0] || undefined,
      createdAtEnd: filters.createdAtRange?.[1] || undefined,
      page: page.current,
      size: page.size
    });
    users.value = data.content || [];
    page.total = Number(data.totalElements || 0);
  } catch (error) {
    ElMessage.error(error.message || "加载用户失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  page.current = 0;
  load();
}

function reset() {
  Object.assign(filters, { phone: "", displayName: "", enabled: "", createdAtRange: [] });
  search();
}

function handlePageChange(pageNo) {
  page.current = pageNo - 1;
  load();
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    phone: row.phone,
    displayName: row.display_name,
    enabled: Boolean(row.enabled)
  });
  dialogVisible.value = true;
}

async function save() {
  if (!form.displayName.trim()) {
    ElMessage.warning("请输入用户昵称");
    return;
  }
  saving.value = true;
  try {
    await updateMallUser(form.id, {
      displayName: form.displayName,
      enabled: form.enabled
    });
    ElMessage.success("用户已更新");
    dialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}
</script>
