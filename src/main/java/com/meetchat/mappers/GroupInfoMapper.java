package com.meetchat.mappers;

import com.meetchat.entity.po.GroupInfo;

/**
 * 群聊信息表 数据库操作接口
 */
public interface GroupInfoMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据GroupId获取群信息
     */
    T selectByGroupId(@org.apache.ibatis.annotations.Param("groupId") String groupId);

    /**
     * 根据GroupId更新
     */
    Integer updateByGroupId(@org.apache.ibatis.annotations.Param("bean") T t,
                            @org.apache.ibatis.annotations.Param("groupId") String groupId);
}
