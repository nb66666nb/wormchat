package com.meetchat.entity.enums;

import com.meetchat.utils.StringTools;
import org.omg.CORBA.UNKNOWN;

public enum UserSexEnum {
    BOY(1,"男"),
    GIRL(0,"女"),
    UNKNOWN(2,"未知");
    UserSexEnum(Integer status,String desc){
        this.status = status;
        this.desc = desc;
 }
    public Integer getStatus() {
        return status;
    }
    public String getDesc() {
        return desc;
    }

    private  Integer status;
    private  String desc;
    public static UserSexEnum getByStatus(String status) {
        try {
            if (StringTools.isEmpty(status)) {
                return null;
            }
            return UserSexEnum.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static UserSexEnum getByStatus(Integer status) {
        for (UserSexEnum item : UserSexEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item;
            }
        }
        return null;
    }
}