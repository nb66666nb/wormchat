<template>
  <div v-if="visible" class="emoji-panel">
    <span
      v-for="emoji in emojiList"
      :key="emoji"
      class="emoji-item"
      @click="handleSelect(emoji)"
    >{{ emoji }}</span>
  </div>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['select'])

const emojiList = [
  '😀','😂','🤣','😊','😍','🥰','😘','😜','🤔','😎',
  '👍','👏','🎉','❤️','🔥','💯','✅','🙏','💪','👋',
  '😢','😡','🤯','😱','🥳','😴','🤗','🫡','😤','🫠',
  '📌','📎','💡','⭐','🚀','🎯','📝','🔔','💬','🤝'
]

const handleSelect = (emoji) => {
  emit('select', emoji)
}
</script>

<style lang="scss" scoped>
.emoji-panel {
  display: flex; flex-wrap: wrap; gap: 4px;
  padding: 8px 10px; background: #ffffff;
  border: 1px solid #e8eaed; border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  max-width: 320px;

  .emoji-item {
    width: 28px; height: 28px;
    display: flex; align-items: center; justify-content: center;
    font-size: 18px; cursor: pointer; border-radius: 4px;
    transition: background 0.15s;
    &:hover { background: #f1f3f4; }
  }
}
</style>
