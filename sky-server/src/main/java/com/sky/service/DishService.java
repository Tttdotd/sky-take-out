package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;

public interface DishService {
    /**
     * 新增菜品和口味数据
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO);

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 批量删除菜品
     * @param ids 菜品id，多个id用逗号分隔，例如：1,2,3
     */
    void deleteBatch(String ids);

    /**
     * 修改菜品信息和口味数据
     * @param dishDTO
     */
    void updateWithFlavor(DishDTO dishDTO);

    /**
     * 起售、停售菜品
     * @param status 状态：0停售 1起售
     * @param id 菜品id
     */
    void startOrStop(Integer status, Long id);
}
