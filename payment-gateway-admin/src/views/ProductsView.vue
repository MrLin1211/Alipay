<template>
  <section>
    <el-card shadow="never" class="toolbar-card">
      <el-form inline>
        <el-form-item label="所属商家">
          <el-select v-model="filters.merchantNo" clearable filterable placeholder="全部商家" class="merchant-select">
            <el-option v-for="merchant in merchants" :key="merchant.merchant_no" :label="merchant.merchant_name" :value="merchant.merchant_no">
              <span>{{ merchant.merchant_name }}</span>
              <span class="option-extra">{{ merchant.merchant_no }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="商品编码">
          <el-input v-model="filters.productCode" clearable />
        </el-form-item>
        <el-form-item label="商品名称">
          <el-input v-model="filters.productName" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.category" clearable placeholder="全部分类" class="status-select">
            <el-option v-for="item in categories" :key="item.category_code" :label="item.category_name" :value="item.category_code" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" class="status-select">
            <el-option v-for="item in productStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="openCreate">新增商品</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="products" border>
        <el-table-column label="主图" width="88" align="center">
          <template #default="{ row }">
            <el-image
              v-if="mainImage(row)"
              class="table-product-image"
              :src="mainImage(row)"
              :preview-src-list="parseImageUrls(row)"
              preview-teleported
              fit="cover"
            />
            <div v-else class="table-product-image empty">无图</div>
          </template>
        </el-table-column>
        <el-table-column prop="product_code" label="商品编码" min-width="130" />
        <el-table-column prop="product_name" label="商品名称" min-width="180" />
        <el-table-column prop="merchant_no" label="所属商家" min-width="140">
          <template #default="{ row }">{{ merchantName(row.merchant_no) }}</template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ categoryName(row.category) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="130">
          <template #default="{ row }">
            <div class="product-price-cell">
              <strong>{{ money(skuMinPrice(row)) }}</strong>
              <span v-if="enabledSkus(row).length > 1">起</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="110">
          <template #default="{ row }">
            <span :class="['stock-value', { low: skuTotalStock(row) <= 10 }]">{{ skuTotalStock(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="SKU" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ enabledSkus(row).length || 1 }} 个</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="productTagType(row.status)">{{ labelOf(productStatusOptions, row.status) }}</el-tag>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑商品' : '新增商品'" width="680px" class="product-dialog">
      <el-form :model="form" label-width="96px" class="product-form">
        <section class="form-section">
          <div class="form-section-head">
            <strong>基础信息</strong>
            <span>编辑商品时所属商家只读，避免影响历史订单和购物车</span>
          </div>
          <el-form-item label="所属商家" required>
            <el-input v-if="editingId" :model-value="merchantName(form.merchantNo)" disabled />
            <el-select v-else v-model="form.merchantNo" filterable placeholder="请选择商家" class="full-control">
              <el-option v-for="merchant in merchants" :key="merchant.merchant_no" :label="merchant.merchant_name" :value="merchant.merchant_no">
                <span>{{ merchant.merchant_name }}</span>
                <span class="option-extra">{{ merchant.merchant_no }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="商品编码">
            <el-input
              :model-value="editingId ? form.productCode : '保存后系统自动生成'"
              disabled
            />
          </el-form-item>
          <el-form-item label="商品名称" required>
            <el-input v-model="form.productName" maxlength="128" show-word-limit />
          </el-form-item>
          <el-form-item label="分类" required>
            <el-select v-model="form.category" filterable placeholder="请选择商品分类" class="full-control">
              <el-option v-for="item in categories" :key="item.category_code" :label="item.category_name" :value="item.category_code" />
            </el-select>
          </el-form-item>
          <div class="form-grid">
            <el-form-item label="状态" required>
              <el-select v-model="form.status" class="full-control">
                <el-option v-for="item in productStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </div>
        </section>

        <section class="form-section">
          <div class="form-section-head">
            <strong>SKU 配置</strong>
            <span>列表价格取启用 SKU 最低价，库存取启用 SKU 合计</span>
          </div>
          <el-form-item label="SKU配置" required>
            <div class="sku-editor">
              <el-table :data="form.skus" size="small" border>
                <el-table-column label="规格名称" min-width="140">
                  <template #default="{ row }">
                    <el-input v-model="row.skuName" placeholder="默认规格" />
                  </template>
                </el-table-column>
                <el-table-column label="价格" width="150">
                  <template #default="{ row }">
                    <el-input-number v-model="row.price" :min="0.01" :precision="2" :step="1" class="full-control" />
                  </template>
                </el-table-column>
                <el-table-column label="库存" width="130">
                  <template #default="{ row }">
                    <el-input-number v-model="row.stock" :min="0" :step="1" class="full-control" />
                  </template>
                </el-table-column>
                <el-table-column label="启用" width="80">
                  <template #default="{ row }">
                    <el-switch v-model="row.enabled" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="{ $index }">
                    <el-button link type="danger" :disabled="form.skus.length <= 1" @click="removeSku($index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-button class="sku-add-button" :icon="Plus" @click="addSku">添加SKU</el-button>
            </div>
          </el-form-item>
        </section>

        <section class="form-section">
          <div class="form-section-head">
            <strong>商品图片</strong>
            <span>最多 5 张，第一张作为商城主图</span>
          </div>
          <el-form-item label="商品图片">
            <div class="product-image-field">
              <div v-for="(url, index) in form.imageUrls" :key="url" class="product-image-preview">
                <img :src="url" alt="商品图片" />
                <span v-if="index === 0" class="main-image-badge">主图</span>
                <button class="image-remove" type="button" @click="removeImage(index)">×</button>
              </div>
              <el-upload
                v-if="form.imageUrls.length < 5"
                class="product-image-upload"
                accept="image/*"
                :show-file-list="false"
                :http-request="uploadCoverImage"
              >
                <div class="product-image-placeholder">
                  <el-icon><Plus /></el-icon>
                  <span>上传图片</span>
                </div>
              </el-upload>
              <div class="image-actions">
                <span v-if="uploadingImage">上传中...</span>
                <span v-else>支持 JPG、PNG、WEBP、GIF，最大 5MB</span>
              </div>
            </div>
          </el-form-item>
        </section>

        <section class="form-section">
          <div class="form-section-head">
            <strong>商品描述</strong>
            <span>用于后台维护，商城列表不展示描述</span>
          </div>
          <el-form-item label="商品描述">
            <el-input v-model="form.description" type="textarea" :rows="4" maxlength="1024" show-word-limit />
          </el-form-item>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Plus, RefreshLeft, Search } from "@element-plus/icons-vue";
import { createProduct, fetchCategories, fetchMerchants, fetchProducts, updateProduct, uploadProductImage } from "../api/gatewayAdminApi";

const productStatusOptions = [
  { value: "ON_SALE", label: "上架" },
  { value: "OFF_SALE", label: "下架" },
  { value: "SOLD_OUT", label: "售罄" }
];

const filters = reactive({
  merchantNo: "",
  productCode: "",
  productName: "",
  category: "",
  status: "",
  createdAtRange: []
});
const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
];
const form = reactive(defaultForm());
const page = reactive({ current: 0, size: 20, total: 0 });
const merchants = ref([]);
const categories = ref([]);
const products = ref([]);
const loading = ref(false);
const saving = ref(false);
const uploadingImage = ref(false);
const dialogVisible = ref(false);
const editingId = ref(null);
const MAX_IMAGE_SIZE = 5 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];
const merchantMap = computed(() => new Map(merchants.value.map((merchant) => [merchant.merchant_no, merchant])));
const categoryMap = computed(() => new Map(categories.value.map((item) => [item.category_code, item])));

onMounted(async () => {
  await Promise.all([loadMerchants(), loadCategories()]);
  await load();
});

function defaultForm() {
  return {
    merchantNo: "",
    productCode: "",
    productName: "",
    category: "",
    price: 0.01,
    stock: 0,
    status: "OFF_SALE",
    skus: [{ skuName: "默认规格", price: 0.01, stock: 0, enabled: true }],
    coverImage: "",
    imageUrls: [],
    description: ""
  };
}

function labelOf(options, value) {
  return options.find((item) => item.value === value)?.label || value || "-";
}

function productTagType(status) {
  if (status === "ON_SALE") return "success";
  if (status === "SOLD_OUT") return "warning";
  return "info";
}

function merchantName(merchantNo) {
  const merchant = merchantMap.value.get(merchantNo);
  return merchant ? `${merchant.merchant_name} (${merchantNo})` : merchantNo;
}

function categoryName(code) {
  return categoryMap.value.get(code)?.category_name || code || "-";
}

function enabledSkus(row) {
  const skus = Array.isArray(row.skus) && row.skus.length
    ? row.skus
    : [{ price: row.price, stock: row.stock, enabled: true }];
  return skus.filter((sku) => sku.enabled !== false);
}

function skuMinPrice(row) {
  const prices = enabledSkus(row).map((sku) => Number(sku.price || 0)).filter((price) => price > 0);
  return prices.length ? Math.min(...prices) : Number(row.price || 0);
}

function skuTotalStock(row) {
  return enabledSkus(row).reduce((sum, sku) => sum + Number(sku.stock || 0), 0);
}

function money(value) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

async function loadMerchants() {
  const data = await fetchMerchants({ status: "ACTIVE", page: 0, size: 100 });
  merchants.value = data.content || [];
  if (!form.merchantNo && merchants.value.length) {
    form.merchantNo = merchants.value[0].merchant_no;
  }
}

async function loadCategories() {
  categories.value = await fetchCategories({ enabled: true });
}

async function load() {
  loading.value = true;
  try {
    const data = await fetchProducts({
      merchantNo: filters.merchantNo || undefined,
      productCode: filters.productCode || undefined,
      productName: filters.productName || undefined,
      category: filters.category || undefined,
      status: filters.status || undefined,
      createdAtStart: filters.createdAtRange?.[0] || undefined,
      createdAtEnd: filters.createdAtRange?.[1] || undefined,
      page: page.current,
      size: page.size
    });
    products.value = data.content || [];
    page.total = Number(data.totalElements || 0);
  } catch (error) {
    ElMessage.error(error.message || "加载商品失败");
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
    productCode: "",
    productName: "",
    category: "",
    status: "",
    createdAtRange: []
  });
  search();
}

function handlePageChange(pageNo) {
  page.current = pageNo - 1;
  load();
}

function openCreate() {
  editingId.value = null;
  Object.assign(form, defaultForm(), {
    merchantNo: merchants.value[0]?.merchant_no || "",
    category: categories.value[0]?.category_code || ""
  });
  dialogVisible.value = true;
}

function openEdit(row) {
  editingId.value = row.id;
  Object.assign(form, {
    merchantNo: row.merchant_no,
    productCode: row.product_code,
    productName: row.product_name,
    category: row.category || "",
    price: Number(row.price || 0.01),
    stock: Number(row.stock || 0),
    status: row.status || "OFF_SALE",
    skus: normalizeSkus(row),
    coverImage: row.cover_image || "",
    imageUrls: parseImageUrls(row),
    description: row.description || ""
  });
  dialogVisible.value = true;
}

function validateForm() {
  if (!form.merchantNo) return "请选择所属商家";
  if (!form.productName.trim()) return "请输入商品名称";
  if (!form.category) return "请选择商品分类";
  if (!form.skus.length) return "请至少配置一个SKU";
  if (!form.skus.some((sku) => sku.enabled)) return "请至少启用一个SKU";
  if (form.skus.some((sku) => !sku.skuName.trim())) return "请输入SKU规格名称";
  if (form.skus.some((sku) => !sku.price || Number(sku.price) < 0.01)) return "请输入有效SKU价格";
  if (form.skus.some((sku) => Number(sku.stock) < 0)) return "SKU库存不能小于0";
  return "";
}

async function uploadCoverImage({ file, onSuccess, onError }) {
  const validationMessage = validateImageFile(file);
  if (validationMessage) {
    const error = new Error(validationMessage);
    ElMessage.error(validationMessage);
    onError?.(error);
    return;
  }
  uploadingImage.value = true;
  try {
    if (form.imageUrls.length >= 5) {
      ElMessage.warning("最多上传5张图片");
      return;
    }
    const data = await uploadProductImage(file);
    form.imageUrls.push(data.url);
    form.coverImage = form.imageUrls[0] || "";
    ElMessage.success("图片已上传");
    onSuccess?.(data);
  } catch (error) {
    ElMessage.error(uploadErrorMessage(error));
    onError?.(error);
  } finally {
    uploadingImage.value = false;
  }
}

function validateImageFile(file) {
  if (!file) return "请选择图片";
  if (file.size > MAX_IMAGE_SIZE) return "图片不能超过5MB，请压缩后重新上传";
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) return "仅支持 JPG、PNG、WEBP、GIF 图片";
  return "";
}

function uploadErrorMessage(error) {
  if (error?.code === "ECONNABORTED") {
    return "图片上传超时，请压缩图片或检查网络后重试";
  }
  return error?.response?.data?.message || error?.message || "图片上传失败";
}

function removeImage(index) {
  form.imageUrls.splice(index, 1);
  form.coverImage = form.imageUrls[0] || "";
}

function parseImageUrls(row) {
  try {
    const images = JSON.parse(row.product_images || "[]");
    if (Array.isArray(images) && images.length > 0) {
      return images.filter(Boolean).slice(0, 5);
    }
  } catch {
    // 兼容历史数据。
  }
  return row.cover_image ? [row.cover_image] : [];
}

function mainImage(row) {
  return parseImageUrls(row)[0] || row.cover_image || "";
}

function normalizeSkus(row) {
  const skus = Array.isArray(row.skus) ? row.skus : [];
  if (skus.length === 0) {
    return [{ skuName: "默认规格", price: Number(row.price || 0.01), stock: Number(row.stock || 0), enabled: true }];
  }
  return skus.map((sku) => ({
    skuName: sku.skuName || sku.sku_name || "默认规格",
    skuCode: sku.skuCode || sku.sku_code || "",
    price: Number(sku.price || 0.01),
    stock: Number(sku.stock || 0),
    enabled: sku.enabled !== false
  }));
}

function addSku() {
  form.skus.push({ skuName: `规格${form.skus.length + 1}`, price: form.skus[0]?.price || 0.01, stock: 0, enabled: true });
}

function removeSku(index) {
  if (form.skus.length > 1) {
    form.skus.splice(index, 1);
  }
}

async function save() {
  const message = validateForm();
  if (message) {
    ElMessage.warning(message);
    return;
  }

  saving.value = true;
  const payload = { ...form, coverImage: form.imageUrls[0] || "", imageUrls: form.imageUrls, skus: form.skus.map((sku, index) => ({ ...sku, sortOrder: index })) };
  try {
    if (editingId.value) {
      await updateProduct(editingId.value, payload);
      ElMessage.success("商品已更新");
    } else {
      await createProduct(payload);
      ElMessage.success("商品已新增");
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

<style scoped>
.product-image-field {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
}

.product-image-upload {
  flex-shrink: 0;
}

.product-image-preview,
.product-image-placeholder {
  width: 112px;
  height: 112px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.product-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-image-placeholder {
  flex-direction: column;
  gap: 8px;
  color: var(--el-text-color-secondary);
}

.image-actions {
  min-width: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.main-image-badge {
  position: absolute;
  left: 6px;
  top: 6px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(15, 118, 110, 0.9);
  color: #fff;
  font-size: 12px;
}

.image-remove {
  position: absolute;
  right: 6px;
  top: 6px;
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  cursor: pointer;
}
</style>
