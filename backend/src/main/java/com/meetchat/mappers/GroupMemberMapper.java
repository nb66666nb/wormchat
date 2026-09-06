package com.meetchat.mappers;

import com.meetchat.entity.po.GroupInfo;
import com.meetchat.entity.po.GroupMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 群成员表 数据库操作接口
 */
public interface GroupMemberMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据GroupId和UserId获取成员
     */
    T selectByGroupIdAndUserId(@Param("groupId") String groupId, @Param("userId") String userId);

    /**
     * 获取群内所有正常成员（status=0）
     */
    List<T> selectActiveMembersByGroupId(@Param("groupId") String groupId);

    /**
     * 查询用户加入的所有正常状态的群ID列表（status=0 且群未解散）
     * 用于用户上线时自动加入各群的 ChannelGroup
     */
    List<String> selectActiveGroupIdsByUserId(@Param("userId") String userId);

    /**
     * 查询用户的群聊列表（返回群信息）
     * @param userId    用户ID
     * @param ownerOnly true=仅我创建的（role=OWNER），false=仅我加入的（role!=OWNER）
     */
    List<GroupInfo> selectGroupsByUserId(@Param("userId") String userId,
                                         @Param("ownerOnly") Boolean ownerOnly);

    /**
     * 根据GroupId和UserId更新
     */
    Integer updateByGroupIdAndUserId(@Param("bean") T t,
                                     @Param("groupId") String groupId,
                                     @Param("userId") String userId);

    /**
     * 根据GroupId和UserId更新角色
     */
    Integer updateRole(@Param("groupId") String groupId,
                       @Param("userId") String userId,
                       @Param("role") String role);

    /**
     * 根据GroupId和UserId更新状态
     */
    Integer updateStatus(@Param("groupId") String groupId,
                         @Param("userId") String userId,
                         @Param("status") Integer status);

    /**
     * 根据GroupId删除所有成员
     */
    Integer deleteByGroupId(@Param("groupId") String groupId);

    /**
     * 统计单个群的正常成员数（status=0）
     */
    Integer countActiveMembersByGroupId(@Param("groupId") String groupId);

    /**
     * 批量统计多个群的正常成员数（status=0）
     * 返回 List&lt;Map&gt;，key：groupId、memberCount
     */
    List<Map<String, Object>> countActiveMembersByGroupIds(@Param("groupIds") List<String> groupIds);
}
