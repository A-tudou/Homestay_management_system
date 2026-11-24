<script setup>
/**
 * 后台房型管理：分页查询房型，支持上架/下架、编辑与删除。
 * 新增和编辑复用同一个弹窗与表单，靠 form.id 是否为空区分提交 create 还是 update。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { adminRoomTypeApi } from '../../api'
import { formatDateTime } from '../../utils/date'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filter = reactive({ keyword: '', status: null })

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
// 弹窗表单：id 为 null 时走新增，有值时为编辑目标
const form = reactive({
  id: null,
  name: '',
  description: '',
  basePrice: 299,
  maxGuests: 2,
  bedType: '',
  area: null,
  coverImage: ''
})

const rules = {
  name: [
    { required: true, message: '请输入房型名称', trigger: 'blur' },
    { max: 50, message: '房型名称不能超过 50 个字符', trigger: 'blur' }
  ],
  basePrice: [{ required: true, message: '请输入基准价', trigger: 'change' }],
  maxGuests: [{ required: true, message: '请输入最大入住人数', trigger: 'change' }],
  description: [{ max: 500, message: '描述不能超过 500 个字符', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const data = await adminRoomTypeApi.page({
      page: page.value,
      size: size.value,
      keyword: filter.keyword.trim() || undefined,
      // status 为 0 也要作为条件提交，因此用 ?? 而不是 ||
      status: filter.status ?? undefined
    })
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  filter.keyword = ''
  filter.status = null
  search()
}

// 新增：重置表单并清空校验态，避免残留上一次编辑的错误提示
function openCreate() {
  Object.assign(form, {
    id: null,
    name: '',
    description: '',
    basePrice: 299,
    maxGuests: 2,
    bedType: '',
    area: null,
    coverImage: ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

// 编辑：把行数据回填到同一个表单，id 有值即走 update
function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    description: row.description || '',
    basePrice: Number(row.basePrice),
    maxGuests: row.maxGuests,
    bedType: row.bedType || '',
    area: row.area,
    coverImage: row.coverImage || ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 可选字段留空时统一转 undefined，避免把空串写给后端
  const payload = {
    name: form.name.trim(),
    description: form.description.trim() || undefined,
    basePrice: form.basePrice,
    maxGuests: form.maxGuests,
    bedType: form.bedType.trim() || undefined,
    area: form.area || undefined,
    coverImage: form.coverImage.trim() || undefined
  }

  saving.value = true
  try {
    if (form.id) {
      await adminRoomTypeApi.update(form.id, payload)
      ElMessage.success('房型已更新')
    } else {
      await adminRoomTypeApi.create(payload)
      ElMessage.success('房型已创建')
    }
    dialogVisible.value = false
    load()
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  } finally {
    saving.value = false
  }
}

// 上架 / 下架：取反当前状态提交
async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  try {
    await adminRoomTypeApi.updateStatus(row.id, next)
    ElMessage.success(next === 1 ? '房型已上架' : '房型已下架')
    load()
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  }
}

async function remove(row) {
  // 删除前明确告知约束：房型下还有房间时后端会拒绝
  try {
    await ElMessageBox.confirm(`确定删除房型「${row.name}」吗？房型下仍有房间时无法删除。`, '删除房型', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await adminRoomTypeApi.remove(row.id)
    ElMessage.success('房型已删除')
    // 删掉当前页最后一条时回退一页，防止停在空页
    if (list.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    load()
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="admin-card">
      <div class="admin-toolbar">
        <el-input
          v-model="filter.keyword"
          placeholder="搜索房型名称"
          clearable
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filter.status" placeholder="上架状态" clearable @change="search">
          <el-option label="上架中" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button class="grow" type="primary" plain @click="openCreate">新增房型</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="封面" width="90">
          <template #default="{ row }">
            <el-image v-if="row.coverImage" class="cover-thumb" :src="row.coverImage" fit="cover">
              <template #error>
                <div class="cover-thumb" />
              </template>
            </el-image>
            <div v-else class="cover-thumb" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="房型名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="基准价" width="110">
          <template #default="{ row }">¥{{ Number(row.basePrice).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="maxGuests" label="可住" width="80">
          <template #default="{ row }">{{ row.maxGuests }} 人</template>
        </el-table-column>
        <el-table-column prop="bedType" label="床型" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.bedType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="area" label="面积" width="90">
          <template #default="{ row }">{{ row.area ? `${row.area} ㎡` : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '上架中' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑房型' : '新增房型'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="房型名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit placeholder="如：山景大床房" />
        </el-form-item>
        <div class="admin-form-row">
          <el-form-item label="基准价" prop="basePrice">
            <el-input-number v-model="form.basePrice" :min="0.01" :precision="2" :step="10" controls-position="right" />
          </el-form-item>
          <el-form-item label="可住人数" prop="maxGuests">
            <el-input-number v-model="form.maxGuests" :min="1" :max="20" controls-position="right" />
          </el-form-item>
        </div>
        <div class="admin-form-row">
          <el-form-item label="床型">
            <el-input v-model="form.bedType" maxlength="30" placeholder="如：1.8m 大床" />
          </el-form-item>
          <el-form-item label="面积（㎡）">
            <el-input-number v-model="form.area" :min="1" :max="2000" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="封面图">
          <el-input v-model="form.coverImage" maxlength="255" placeholder="图片地址，可留空" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="介绍一下这个房型的亮点"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
