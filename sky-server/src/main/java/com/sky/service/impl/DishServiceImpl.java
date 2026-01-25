package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /**
     * 新增菜品和对应口味
     * @param dishDTO
     */
    @Transactional
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {

        //创建Dish实体
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //向菜品表中插入一条数据
        dishMapper.insert(dish);

        //获取菜品id
        Long dishId = dish.getId();

        //向口味表中插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {

            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }

            dishFlavorMapper.insert(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        //开启分页
        PageHelper.startPage(
                dishPageQueryDTO.getPage(),
                dishPageQueryDTO.getPageSize()
        );

        //执行查询（关联查询分类表获取分类名称）
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        //封装分页结果
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids 菜品id，多个id用逗号分隔，例如：1,2,3
     */
    @Transactional
    @Override
    public void deleteBatch(String ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 将字符串转换为Long列表
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 判断是否可以删除：是否存在起售中的菜品
        for (Long id : idList) {
            Dish dish = dishMapper.selectById(id);
            if (dish == null) {
                continue;
            }
            // 起售中的菜品不能删除
            if (dish.getStatus() == 1) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断是否可以删除：是否被套餐关联
        for (Long id : idList) {
            Integer count = dishMapper.countByDishId(id);
            if (count > 0) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }
        }

        // 删除菜品关联的口味数据（使用 in 条件一次性删除）
        LambdaQueryWrapper<DishFlavor> flavorWrapper = Wrappers.lambdaQuery(DishFlavor.class)
                .in(DishFlavor::getDishId, idList);
        dishFlavorMapper.delete(flavorWrapper);

        // 删除菜品数据（使用 MyBatis-Plus 提供的批量删除方法）
        dishMapper.deleteBatchIds(idList);
    }

    /**
     * 修改菜品信息和口味数据
     * @param dishDTO
     */
    @Transactional
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        // 更新菜品基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.updateById(dish);

        // 获取菜品id
        Long dishId = dishDTO.getId();

        // 删除旧的口味数据
        LambdaQueryWrapper<DishFlavor> flavorWrapper = Wrappers.lambdaQuery(DishFlavor.class)
                .eq(DishFlavor::getDishId, dishId);
        dishFlavorMapper.delete(flavorWrapper);

        // 插入新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }
            dishFlavorMapper.insert(flavors);
        }

    }

    /**
     * 起售、停售菜品
     * @param status 状态：0停售 1起售
     * @param id 菜品id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 更新菜品状态
        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);

        dishMapper.updateById(dish);
    }

    /**
     * 根据分类id查询菜品列表
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    @Override
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
