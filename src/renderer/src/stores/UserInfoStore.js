import { reactive } from 'vue'

const USER_INFO_KEY = 'userInfo'

const state = reactive({
  token: '',
  userName: '',
  admin: '',
  sex: '',
  userId: '',
  email: ''
})

const UserInfoStore = {
  state,

  getUserInfo() {
    const userInfo = localStorage.getItem(USER_INFO_KEY)
    return userInfo ? JSON.parse(userInfo) : null
  },

  setUserInfo(userInfo) {
    state.token = userInfo.token || ''
    state.userName = userInfo.userName || ''
    state.admin = userInfo.admin || ''
    state.sex = userInfo.sex || ''
    state.userId = userInfo.userId || ''
    state.email = userInfo.email || ''
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(state))
  },

  getToken() {
    return state.token
  },

  getUserId() {
    return state.userId
  },

  clearUserInfo() {
    state.token = ''
    state.userName = ''
    state.admin = ''
    state.sex = ''
    state.userId = ''
    state.email = ''
    localStorage.removeItem(USER_INFO_KEY)
  },

  init() {
    const userInfo = this.getUserInfo()
    if (userInfo) {
      Object.assign(state, userInfo)
    }
  }
}

UserInfoStore.init()

export default UserInfoStore