<template>
  <div class="ai-assistant-panel">
    <!-- 聊天消息区域 -->
    <div class="chat-messages" ref="messagesContainer" @scroll="handleScroll">
      <div v-if="messages.length === 0" class="empty-state">
        <div class="empty-icon">
          <el-icon :size="48" color="#1a73e8"><ChatDotRound /></el-icon>
        </div>
        <h3 class="empty-title">AI 助手</h3>
        <p class="empty-desc">有什么可以帮你的？</p>
        <div class="quick-questions">
          <div
            v-for="q in quickQuestions"
            :key="q"
            class="quick-question"
            @click="handleQuickQuestion(q)"
          >
            {{ q }}
          </div>
        </div>
      </div>

      <div v-else class="message-list">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message-item', msg.role === 'user' ? 'user-message' : 'ai-message']"
        >
          <div class="message-avatar">
            <el-icon v-if="msg.role === 'ai'" :size="20" color="#1a73e8">
              <ChatDotRound />
            </el-icon>
            <el-icon v-else :size="20" color="#5f6368">
              <User />
            </el-icon>
          </div>
          <div class="message-content">
            <div class="message-text">{{ msg.content }}</div>
            <div class="message-time">{{ msg.time }}</div>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="isLoading" class="message-item ai-message">
          <div class="message-avatar">
            <el-icon :size="20" color="#1a73e8"><ChatDotRound /></el-icon>
          </div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <div class="input-wrapper">
        <textarea
          v-model="inputMessage"
          :placeholder="isLoading ? 'AI 正在思考...' : '输入你的问题...'"
          :disabled="isLoading"
          @keydown.enter.exact="handleSend"
          @input="autoResize"
          ref="inputRef"
          class="message-input"
          rows="1"
        />
        <el-button
          type="primary"
          :disabled="!inputMessage.trim() || isLoading"
          @click="handleSend"
          class="send-btn"
          circle
        >
          <el-icon><Promotion /></el-icon>
        </el-button>
      </div>
      <div class="input-hint">按 Enter 发送，Shift+Enter 换行</div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ChatDotRound, User, Promotion } from '@element-plus/icons-vue'

const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const messagesContainer = ref(null)
const inputRef = ref(null)

const quickQuestions = [
  '如何创建一个新的通话？',
  '如何邀请通话成员？',
  '通话记录在哪里查看？'
]

const getCurrentTime = () => {
  const now = new Date()
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const autoResize = () => {
  const el = inputRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 120) + 'px'
  }
}

const handleSend = () => {
  const content = inputMessage.value.trim()
  if (!content || isLoading.value) return

  messages.value.push({
    role: 'user',
    content,
    time: getCurrentTime()
  })

  inputMessage.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }
  scrollToBottom()

  // 模拟 AI 回复
  simulateAIResponse(content)
}

const handleQuickQuestion = (q) => {
  inputMessage.value = q
  handleSend()
}

const simulateAIResponse = async (userMessage) => {
  isLoading.value = true
  scrollToBottom()

  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 1000))

  const responses = {
    '如何创建一个新的通话？': '创建通话非常简单：\n1. 点击左侧的"创建通话"按钮\n2. 填写通话名称和相关信息\n3. 点击"开始通话"即可',
    '如何邀请通话成员？': '邀请通话成员有以下几种方式：\n1. 在通话中点击"邀请"按钮\n2. 通过通话号分享链接\n3. 在通讯录中选择联系人直接邀请\n\n通话成员收到邀请后即可加入通话。',
    '通话记录在哪里查看？': '查看通话记录：\n1. 点击左侧"通话记录"按钮\n2. 在列表中选择你要查看的通话\n3. 即可查看通话的详细信息和聊天记录\n\n所有通话记录都会自动保存，方便随时回顾。'
  }

  const reply = responses[userMessage] || `收到你的问题："${userMessage}"\n\n这是一个演示回复。实际使用时，这里会连接 AI 服务来提供智能回答。`

  messages.value.push({
    role: 'ai',
    content: reply,
    time: getCurrentTime()
  })

  isLoading.value = false
  scrollToBottom()
}

const handleScroll = () => {
  // 可以在这里实现滚动加载更多历史记录
}

defineExpose({
  messages,
  clearMessages: () => { messages.value = [] }
})
</script>

<style lang="css" scoped>
.ai-assistant-panel {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  overflow: hidden;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  background: #f8f9fa;
}

.chat-messages::-webkit-scrollbar {
  width: 5px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background-color: #dadce0;
  border-radius: 3px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px 20px;
}

.empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(26, 115, 232, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #202124;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: #5f6368;
  margin: 0 0 24px;
}

.quick-questions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  max-width: 320px;
}

.quick-question {
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e8eaed;
  border-radius: 10px;
  font-size: 13px;
  color: #202124;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.quick-question:hover {
  border-color: #1a73e8;
  background: rgba(26, 115, 232, 0.03);
  color: #1a73e8;
}

/* 消息列表 */
.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.user-message {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.ai-message {
  align-self: flex-start;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f1f3f4;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-message .message-avatar {
  background: #e8eaed;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-text {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-message .message-text {
  background: #ffffff;
  color: #202124;
  border: 1px solid #e8eaed;
  border-top-left-radius: 4px;
}

.user-message .message-text {
  background: #1a73e8;
  color: #ffffff;
  border-top-right-radius: 4px;
}

.message-time {
  font-size: 11px;
  color: #9aa0a6;
  padding: 0 4px;
}

.user-message .message-time {
  text-align: right;
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e8eaed;
  border-radius: 14px;
  border-top-left-radius: 4px;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #dadce0;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    background: #dadce0;
  }
  30% {
    transform: translateY(-6px);
    background: #1a73e8;
  }
}

/* 输入区域 */
.input-area {
  padding: 16px 20px;
  border-top: 1px solid #e8eaed;
  background: #ffffff;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background: #f8f9fa;
  border: 1px solid #e8eaed;
  border-radius: 14px;
  padding: 8px 12px;
  transition: border-color 0.2s ease;
}

.input-wrapper:focus-within {
  border-color: #1a73e8;
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.15);
}

.message-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  line-height: 1.5;
  color: #202124;
  resize: none;
  outline: none;
  padding: 4px 0;
  max-height: 120px;
  font-family: inherit;
}

.message-input::placeholder {
  color: #9aa0a6;
}

.message-input:disabled {
  opacity: 0.6;
}

.send-btn {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  background: #1a73e8;
  border: none;
  color: #ffffff;
}

.send-btn:hover:not(:disabled) {
  background: #1557b0;
}

.send-btn:disabled {
  opacity: 0.5;
}

.input-hint {
  font-size: 11px;
  color: #9aa0a6;
  text-align: center;
  margin-top: 8px;
}
</style>
