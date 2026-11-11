package org.jeecg.modules.pcms.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.pcms.project.entity.PvProjectFileOther;

public interface PvProjectFileOtherMapper extends BaseMapper<PvProjectFileOther> {

    @Update("update pv_project_file_other set del_flag = 1 where del_flag = 0 and id = #{id}")
    void delProjectInfoOtherFile(@Param("id") Integer id);

}
