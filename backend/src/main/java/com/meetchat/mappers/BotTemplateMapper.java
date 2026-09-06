package com.meetchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 *  数据库操作接口
 */
public interface BotTemplateMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据TemplateId更新
	 */
	 Integer updateByTemplateId(@Param("bean") T t,@Param("templateId") String templateId);


	/**
	 * 根据TemplateId删除
	 */
	 Integer deleteByTemplateId(@Param("templateId") String templateId);


	/**
	 * 根据TemplateId获取对象
	 */
	 T selectByTemplateId(@Param("templateId") String templateId);


}