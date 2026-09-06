import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '@/login/Login.vue'
import Register from '@/login/Register.vue'
import Main from '@/Main.vue'
import Meeting from '@/pages/meeting/Meeting.vue'
import MeetingRoom from '@/pages/meetingRoom/MeetingRoom.vue'
import Contacts from '@/pages/contacts/Contacts.vue'
import ContactSearch from '@/pages/contacts/Search.vue'
import MyContact from '@/pages/contacts/MyContact.vue'
import MyRequest from '@/pages/contacts/MyRequest.vue'
import Deal from '@/pages/contacts/Deal.vue'
import MyGroups from '@/pages/contacts/MyGroups.vue'
import Settings from '@/pages/settings/Settings.vue'
import AdminPanel from '@/pages/admin/AdminPanel.vue'
import ChatPage from '@/pages/chat/ChatPage.vue'

const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        {
            path: '/',
            name: 'Login',
            component: Login
        },
        {
            path: '/register',
            name: 'Register',
            component: Register
        },
        {
            path: '/main',
            component: Main,
            children: [
                {
                    path: '',
                    redirect: '/main/meeting'
                },
                {
                    path: 'meeting',
                    name: 'Meeting',
                    component: Meeting
                },
                {
                    path: 'contacts',
                    component: Contacts,
                    children: [
                        {
                            path: '',
                            redirect: 'contacts/search'
                        },
                        {
                            path: 'search',
                            name: 'ContactSearch',
                            component: ContactSearch
                        },
                        {
                            path: 'friends',
                            name: 'ContactFriends',
                            component: MyContact
                        },
                        {
                            path: 'myRequests',
                            name: 'ContactMyRequests',
                            component: MyRequest
                        },
                        {
                            path: 'pendingRequests',
                            name: 'ContactPendingRequests',
                            component: Deal
                        },
                        {
                            path: 'myCreatedGroups',
                            name: 'MyCreatedGroups',
                            component: MyGroups,
                            props: { mode: 'created' }
                        },
                        {
                            path: 'myJoinedGroups',
                            name: 'MyJoinedGroups',
                            component: MyGroups,
                            props: { mode: 'joined' }
                        }
                    ]
                },
                {
                    path: 'settings',
                    name: 'Settings',
                    component: Settings
                },
                {
                    path: 'chat',
                    name: 'Chat',
                    component: ChatPage
                }
            ]
        },
        {
            path: '/meeting-room',
            name: 'MeetingRoom',
            component: MeetingRoom
        },
        {
            path: '/admin-panel',
            name: 'AdminPanel',
            component: AdminPanel
        },
        {
            // 兜底路由：任何未匹配的 /main/xxx 路径都重定向到 meeting，避免页面空白
            path: '/main/:pathMatch(.*)*',
            redirect: '/main/meeting'
        }
    ]
})

export default router
