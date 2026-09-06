package com.meetchat.entity.Dto;

import java.io.Serializable;

/**
 * 好友信息 DTO（包含用户昵称、头像等展示信息）
 */
public class FriendWithUserInfoDto implements Serializable {

    /** 好友用户ID */
    private String friendId;

    /** 好友昵称 */
    private String nickName;

    /** 好友头像文件ID */
    private String avatarFileId;

    /** 好友头像文件路径 */
    private String avatarFilePath;

    /** 我给好友的备注名 */
    private String remarkName;

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatarFileId() {
        return avatarFileId;
    }

    public void setAvatarFileId(String avatarFileId) {
        this.avatarFileId = avatarFileId;
    }

    public String getAvatarFilePath() {
        return avatarFilePath;
    }

    public void setAvatarFilePath(String avatarFilePath) {
        this.avatarFilePath = avatarFilePath;
    }

    public String getRemarkName() {
        return remarkName;
    }

    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }
}
