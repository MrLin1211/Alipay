<template>
  <section>
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="分类编码">
          <el-input v-model="filters.categoryCode" clearable />
        </el-form-item>
        <el-form-item label="分类名称">
          <el-input v-model="filters.categoryName" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.enabled" clearable placeholder="全部状态" class="status-select">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
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
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="openCreate">新增分类</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="categories" border>
        <el-table-column prop="category_code" label="分类编码" min-width="140" />
        <el-table-column prop="category_name" label="分类名称" min-width="160" />
        <el-table-column prop="sort_order" label="排序" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? "启用" : "停用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" min-width="170" />
        <el-table-column prop="updated_at" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="520px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="分类编码" required>
          <el-input
            :model-value="editingId ? form.categoryCode : '保存后系统自动生成'"
            disabled
          />
        </el-form-item>
        <el-form-item label="分类名称" required>
          <el-input v-model="form.categoryName" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :step="10" class="full-control" />
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
import { Plus, RefreshLeft, Search } from "@element-plus/icons-vue";
import { createCategory, fetchCategories, updateCategory } from "../api/gatewayAdminApi";

const filters = reactive({ categoryCode: "", categoryName: "", enabled: "", createdAtRange: [] });
const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
];
const form = reactive(defaultForm());
const categories = ref([]);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const editingId = ref(null);

onMounted(load);

function defaultForm() {
  return { categoryCode: "", categoryName: "", sortOrder: 0, enabled: true };
}

async function load() {
  loading.value = true;
  try {
    categories.value = await fetchCategories({
      categoryCode: filters.categoryCode || undefined,
      categoryName: filters.categoryName || undefined,
      enabled: filters.enabled === "" ? undefined : filters.enabled,
      createdAtStart: filters.createdAtRange?.[0] || undefined,
      createdAtEnd: filters.createdAtRange?.[1] || undefined
    });
  } catch (error) {
    ElMessage.error(error.message || "加载分类失败");
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(filters, { categoryCode: "", categoryName: "", enabled: "", createdAtRange: [] });
  load();
}

function openCreate() {
  editingId.value = null;
  Object.assign(form, defaultForm());
  dialogVisible.value = true;
}

function openEdit(row) {
  editingId.value = row.id;
  Object.assign(form, {
    categoryCode: row.category_code,
    categoryName: row.category_name,
    sortOrder: Number(row.sort_order || 0),
    enabled: Boolean(row.enabled)
  });
  dialogVisible.value = true;
}

function validateForm() {
  if (!form.categoryName.trim()) return "请输入分类名称";
  return "";
}

async function save() {
  const message = validateForm();
  if (message) {
    ElMessage.warning(message);
    return;
  }
  saving.value = true;
  try {
    const payload = { ...form };
    if (editingId.value) {
      await updateCategory(editingId.value, payload);
      ElMessage.success("分类已更新");
    } else {
      const created = await createCategory(payload);
      ElMessage.success(`分类已新增，编码 ${created.categoryCode}`);
    }
    dialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error.message || "保存失败");
  } finally {
    saving.value = false;
  }
}
</script>
