package com.meetchat.entity.enums;

import com.meetchat.utils.StringTools;

public enum UserStatusEnum {
    NORMAL(1,"正常"),
    DISABLE(0,"禁用");
    private  Integer status;
    private  String desc;
  UserStatusEnum(Integer status,String desc){
        this.status = status;
        this.desc = desc;
 }
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static UserStatusEnum getByStatus(String status) {
        try {
            if (StringTools.isEmpty(status)) {
                return null;
            }
            return UserStatusEnum.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static UserStatusEnum getByStatus(Integer status) {
        for (UserStatusEnum item : UserStatusEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item;
            }
        }
        return null;
    }
}