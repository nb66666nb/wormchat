const Api = {
    getImage: "/account/getImage",
    login: "/account/login",
    register: "/account/register",
    logout: "/account/logout",
    getMeetingInfo: "/meeting/getInfo",
    createMeeting: "/meeting/create",
    joinMeeting: "/meeting/join",
    endMeeting: "/meeting/end",
    leaveMeeting: "/meeting/leave",
    getMembers: "/meeting/getMembers",
    getMyMeetings: "/meeting/getMyMeetings",
    sendMessage: "/meeting/sendMessage",
    kickUser: "/meeting/kickUser",
    // 文件传输API
    uploadFile: "/file/upload",
    downloadFile: "/file/download",
    // 头像上传API（机器人/用户共用）
    uploadAvatar: "/avatar/upload",
    // 分片上传API
    uploadCheck: "/file/upload/check",
    uploadChunk: "/file/upload/chunk",
    uploadMerge: "/file/upload/merge",
    // 通讯录相关API
    searchUserById: "/userFriend/search",
    addContact: "/userFriend/addFriend",
    getMyFriends: "/userFriend/getFriendList",
    getNonBotFriends: "/userFriend/getNonBotFriends",
    getFriendRequests: "/userFriend/getFriendRequestList",
    getMyFriendRequests: "/userFriend/getMyFriendRequest",
    manageRequest: "/userFriend/manageRequest",
    manageMyRequest: "/userFriend/manageMyRequest",
    deleteContact: "/userFriend/deleteFriend",
    // 聊天记录API
    getChatMessages: "/chatMessage/getChatMessages",
    // IM消息发送
    sendImMessage: "/chatImMessage/sendImMessage",
    // IM消息撤回
    recallImMessage: "/chatImMessage/recallMessage",
    // IM离线消息拉取（增量同步）
    pullImMessage: "/chatImMessage/pullOffline",
    // 创建机器人
    createRobot: "/chatSession/createRobot",
    // 管理员面板API - 用户管理
    adminUserList: "/admin/userList",
    adminCheckOnline: "/admin/checkOnline",
    adminOnlineUsers: "/admin/onlineUsers",
    adminManageUser: "/admin/mangeUser",
    adminKickUser: "/admin/kickUser",
    // 管理员面板API - 群聊管理
    adminGroupList: "/admin/group/groupList",
    adminGroupDetail: "/admin/group/groupDetail",
    adminManageGroup: "/admin/group/manageGroup",
    // RAG 知识库 API
    ragUpload: "/rag/upload",
    ragList: "/rag/list",
    ragClear: "/rag/clear",
    ragDeleteOne: "/rag/delete-one",
    ragSearch: "/rag/search",
    // 群聊相关API
    createGroup: "/group/create",
    inviteMember: "/group/invite",
    kickMember: "/group/kick",
    setAdmin: "/group/setAdmin",
    updateGroupSettings: "/group/updateSettings",
    dissolveGroup: "/group/dissolve",
    leaveGroup: "/group/leave",
    getGroupInfo: "/group/getInfo",
    getGroupMembers: "/group/getMembers",
    myCreatedGroups: "/group/myCreated",
    myJoinedGroups: "/group/myJoined",
}

export default Api;
