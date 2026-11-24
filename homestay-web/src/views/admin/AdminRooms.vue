<script setup>
/**
 * 后台房间管理：维护具体房间（房间号/楼层/所属房型）及其可用状态。
 * 房间状态只有可用(1)与维修中(0)，与房型上架状态相互独立。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminRoomApi, adminRoomTypeApi } from '../../api'
import { formatDateTime } from '../../utils/date'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filter = reactive({ roomTypeId: null, status: null })
const roomTypes = ref([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
// 弹窗表单：id 为空时新增，有值时编辑
const form = reactive({ id: null, roomTypeId: null, roomNo: '', floor: 1 })

const rules = {
  roomTypeId: [{ required: true, message: '请选择所属房型', trigger: 'change' }],
  roomNo: [
    { required: true, message: '请输入房间号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9-]{1,20}$/, message: '只能是字母、数字或短横线，最长 20 位', trigger: 'blur' }
  ]
}

// 房型列表同时服务于筛选下拉和表单里的「所属房型」选择
async function loadRoomTypes() {
  try {
    const data = await adminRoomTypeApi.page({ page: 1, size: 100 })
    roomTypes.value = data.records || []
  } catch {
    roomTypes.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const data = await adminRoomApi.page({
      page: page.value,
      size: size.value,
      // 0 是有效筛选值，用 ?? 保证不会被当作空值丢弃
      roomTypeId: filter.roomTypeId ?? undefined,
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
  filter.roomTypeId = null
  filter.status = null
  search()
}

// 新增时默认继承当前筛选的房型，减少重复选择
function openCreate() {
  Object.assign(form, {
    id: null,
    roomTypeId: filter.roomTypeId || null,
    roomNo: '',
    floor: 1
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    roomTypeId: row.roomTypeId,
    roomNo: row.roomNo,
    floor: row.floor ?? 1
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    roomTypeId: form.roomTypeId,
    roomNo: form.roomNo.trim(),
    floor: form.floor
  }

  saving.value = true
  try {
    if (form.id) {
      await adminRoomApi.update(form.id, payload)
      ElMessage.success('房间已更新')
    } else {
      await adminRoomApi.create(payload)
      ElMessage.success('房间已创建')
    }
    dialogVisible.value = false
    load()
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  } finally {
    saving.value = false
  }
}

// 在可用与维修中之间切换
async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  try {
    await adminRoomApi.updateStatus(row.id, next)
    ElMessage.success(next === 1 ? '房间已设为可用' : '房间已设为维修中')
    load()
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除房间「${row.roomNo}」吗？`, '删除房间', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await adminRoomApi.remove(row.id)
    ElMessage.success('房间已删除')
    // 当前页被删空时回退一页
    if (list.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    load()
  } catch {
    /* 错误提示由 http 拦截器统一处理 */
  }
}

onMounted(async () => {
  // 先拿房型选项，再加载房间列表，保证表格里的房型名和筛选项可用
  await loadRoomTypes()
  load()
})
</script>

<template>
  <div>
    <div class="admin-card">
      <div class="admin-toolbar">
        <el-select v-model="filter.roomTypeId" placeholder="所属房型" clearable @change="search">
          <el-option v-for="type in roomTypes" :key="type.id" :label="type.name" :value="type.id" />
        </el-select>
        <el-select v-model="filter.status" placeholder="房间状态" clearable @change="search">
          <el-option label="可用" :value="1" />
          <el-option label="维修中" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button class="grow" type="primary" plain @click="openCreate">新增房间</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="roomNo" label="房间号" min-width="120" />
        <el-table-column prop="roomTypeName" label="所属房型" min-width="180" show-overflow-tooltip />
        <el-table-column prop="floor" label="楼层" width="90">
          <template #default="{ row }">{{ row.floor ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" effect="light">
              {{ row.status === 1 ? '可用' : '维修中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '设为维修中' : '设为可用' }}
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑房间' : '新增房间'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="所属房型" prop="roomTypeId">
          <el-select v-model="form.roomTypeId" placeholder="请选择房型" style="width: 100%">
            <el-option v-for="type in roomTypes" :key="type.id" :label="type.name" :value="type.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间号" prop="roomNo">
          <el-input v-model="form.roomNo" maxlength="20" placeholder="如：201" />
        </el-form-item>
        <el-form-item label="楼层">
          <el-input-number v-model="form.floor" :min="-5" :max="200" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
