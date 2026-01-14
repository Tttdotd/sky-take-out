package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.sky.enumeration.OperationType;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    List<Category> list(Integer type);
}
