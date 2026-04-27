<template>
  <div class="user-center">
    <div class="center-card">
      <div class="card-header">
        <div class="avatar-circle">
          {{ displayName.charAt(0).toUpperCase() }}
        </div>
        <h2 class="user-name">{{ displayName }}</h2>
        <span class="role-badge" :class="roleClass">{{ roleLabel }}</span>
      </div>

      <div class="info-section">
        <div class="info-row">
          <span class="info-label">用户名</span>
          <span class="info-value">{{ user?.username || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">邮箱</span>
          <span class="info-value">{{ user?.email || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">角色</span>
          <span class="info-value">{{ roleLabel }}</span>
        </div>
      </div>

      <div v-if="user?.role === 'NORMAL_USER'" class="upgrade-section">
        <div v-if="upgradeStatus === 'PENDING'" class="upgrade-status pending">
          <div class="status-icon">⏳</div>
          <div>
            <strong>升级申请已提交</strong>
            <p class="status-desc">管理员正在审核您的申请，请耐心等待</p>
          </div>
        </div>
        <div v-else-if="upgradeStatus === 'APPROVED'" class="upgrade-status approved">
          <div class="status-icon">✅</div>
          <div>
            <strong>升级申请已通过</strong>
            <p class="status-desc">您已成功升级为高级用户</p>
          </div>
        </div>
        <div v-else-if="upgradeStatus === 'REJECTED'" class="upgrade-status rejected">
          <div class="status-icon">❌</div>
          <div>
            <strong>升级申请未通过</strong>
            <p class="status-desc">您的升级申请被拒绝，如需重新申请请联系管理员</p>
          </div>
        </div>
        <button v-else class="upgrade-btn" @click="handleUpgrade" :disabled="upgradeLoading">
          <span v-if="upgradeLoading" class="spinner-small"></span>
          <span v-else>申请升级为高级用户</span>
        </button>
        <p v-if="upgradeMessage" class="upgrade-message" :class="upgradeMessageType">{{ upgradeMessage }}</p>
      </div>

      <div class="actions-section">
        <button class="action-btn logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { submitUpgradeRequest, getUpgradeRequestStatus } from '../api/auth'

const router = useRouter()
const { user, logout } = useAuth()

const upgradeStatus = ref('')
const upgradeLoading = ref(false)
const upgradeMessage = ref('')
const upgradeMessageType = ref('')

const displayName = computed(() => user.value?.username || '用户')
const roleLabel = computed(() => {
  const role = user.value?.role
  if (role === 'ADMIN') return '管理员'
  if (role === 'PREMIUM_USER') return '高级用户'
  return '普通用户'
})
const roleClass = computed(() => {
  const role = user.value?.role
  if (role === 'ADMIN') return 'role-admin'
  if (role === 'PREMIUM_USER') return 'role-premium'
  return 'role-normal'
})

onMounted(async () => {
  try {
    const response = await getUpgradeRequestStatus()
    if (response.data.success && response.data.data) {
      upgradeStatus.value = response.data.data.status
    }
  } catch (e) {
    console.error('获取升级状态失败:', e)
  }
})

async function handleUpgrade() {
  upgradeLoading.value = true
  upgradeMessage.value = ''
  try {
    const response = await submitUpgradeRequest()
    upgradeStatus.value = 'PENDING'
    upgradeMessage.value = '升级申请已提交，请等待管理员审核'
    upgradeMessageType.value = 'success'
  } catch (e) {
    upgradeMessage.value = e.response?.data?.message || e.message || '提交失败'
    upgradeMessageType.value = 'error'
  } finally {
    upgradeLoading.value = false
  }
}

function handleLogout() {
  logout()
}
</script>

<style scoped>
.user-center {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - var(--topbar-height));
  padding: 20px;
  background: var(--bg-primary);
}

.center-card {
  width: 100%;
  max-width: 480px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  padding: 36px;
  box-shadow: var(--shadow-lg);
}

.card-header {
  text-align: center;
  margin-bottom: 28px;
}

.avatar-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--accent-gradient);
  color: #fff;
  font-size: 28px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.user-name {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.role-badge {
  display: inline-block;
  padding: 4px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
}

.role-admin {
  background: rgba(234, 67, 53, 0.12);
  color: #ea4335;
}

.role-premium {
  background: rgba(52, 168, 83, 0.12);
  color: #34a853;
}

.role-normal {
  background: rgba(66, 133, 244, 0.12);
  color: #4285f4;
}

.info-section {
  border-top: 1px solid var(--border-primary);
  padding-top: 20px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
}

.info-label {
  font-size: 14px;
  color: var(--text-tertiary);
}

.info-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.upgrade-section {
  border-top: 1px solid var(--border-primary);
  padding-top: 20px;
  margin-bottom: 20px;
}

.upgrade-status {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
}

.upgrade-status.pending {
  background: rgba(251, 188, 4, 0.1);
  border: 1px solid rgba(251, 188, 4, 0.2);
}

.upgrade-status.approved {
  background: rgba(52, 168, 83, 0.1);
  border: 1px solid rgba(52, 168, 83, 0.2);
}

.upgrade-status.rejected {
  background: rgba(234, 67, 53, 0.1);
  border: 1px solid rgba(234, 67, 53, 0.2);
}

.status-icon {
  font-size: 24px;
  line-height: 1;
}

.status-desc {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.upgrade-btn {
  width: 100%;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--accent-primary);
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: opacity var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
}

.upgrade-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.upgrade-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.upgrade-message {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
}

.upgrade-message.success {
  background: rgba(52, 168, 83, 0.1);
  color: #34a853;
}

.upgrade-message.error {
  background: rgba(234, 67, 53, 0.1);
  color: #ea4335;
}

.actions-section {
  border-top: 1px solid var(--border-primary);
  padding-top: 20px;
}

.action-btn {
  width: 100%;
  padding: 12px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.logout-btn {
  background: transparent;
  border: 1px solid var(--border-primary);
  color: var(--text-secondary);
}

.logout-btn:hover {
  background: rgba(234, 67, 53, 0.08);
  border-color: rgba(234, 67, 53, 0.3);
  color: #ea4335;
}

.spinner-small {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
