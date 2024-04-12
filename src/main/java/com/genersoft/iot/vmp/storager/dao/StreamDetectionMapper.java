package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.opencv.frame.bean.StreamDetectionItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface StreamDetectionMapper {
    @Insert("INSERT INTO wvp_record_detection (app, stream, d_type, act_time, act_type) VALUES(#{app}, #{stream}, #{dType}, #{actTime}, #{actType}) ")
    int save(StreamDetectionItem detectionItem);

    /**
     *
     * @param app 流名称 如rtp
     * @param stream 流ID
     * @return 所有事件时间段
     */
    @Select("SELECT * FROM wvp_record_detection WHERE app = #{app} AND stream = #{stream}")
    List<StreamDetectionItem> selectAllAction(String app, String stream);

    /**
     * 某个时间段内的所有动作事件
     * @param start 事件起始时间
     * @param end 事件结束时间
     */
    @Select("SELECT * FROM wvp_record_detection WHERE app = #{app} AND stream = #{stream} AND  act_time BETWEEN #{start} AND #{end}")
    List<StreamDetectionItem> queryStreamAction(String app, String stream, String start, String end);

    /**
     * 删除某个流的所有记录
     */
    @Delete("DELETE FROM wvp_record_detection WHERE app = #{app} AND stream = #{stream}")
    int deleteAll(String app, String stream);

    /**
     * 删除某个流某时间以前的记录
     * @param time 时间
     * @return 删除条数
     */
    @Delete("DELETE FROM wvp_record_detection WHERE app = #{app} AND stream = #{stream} AND act_time <= #{time}")
    int deleteOlder(String app, String stream, String time);

}
