package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.SetMealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetMealServiceImpl implements SetMealService {

    @Autowired
    private SetMealMapper setMealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        //开启分页
        PageHelper.startPage(
                setmealPageQueryDTO.getPage(),
                setmealPageQueryDTO.getPageSize()
        );

        //执行查询（关联查询分类表获取分类名称）
        Page<SetmealVO> page = setMealMapper.pageQuery(setmealPageQueryDTO);

        //封装分页结果
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void saveWithDish(SetmealDTO setmealDTO) {
        //创建Setmeal实体
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //向套餐表中插入一条数据
        setMealMapper.insert(setmeal);

        //获取套餐id
        Long setmealId = setmeal.getId();

        //向套餐菜品关系表中插入n条数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmealId);
            }

            setmealDishMapper.insert(setmealDishes);
        }
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        //判断是否可以删除：是否存在起售中的套餐
        for (Long id : ids) {
            Setmeal setmeal = setMealMapper.selectById(id);
            if (setmeal == null) {
                continue;
            }
            //起售中的套餐不能删除
            if (setmeal.getStatus() == 1) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //删除套餐数据
        for (Long id : ids) {
            setMealMapper.deleteById(id);
        }

        //删除套餐菜品关系数据
        LambdaQueryWrapper<SetmealDish> wrapper = Wrappers.lambdaQuery(SetmealDish.class)
                .in(SetmealDish::getSetmealId, ids);
        setmealDishMapper.delete(wrapper);
    }

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        //查询套餐基本信息
        Setmeal setmeal = setMealMapper.selectById(id);
        if (setmeal == null) {
            return null;
        }

        //查询套餐关联的菜品信息
        LambdaQueryWrapper<SetmealDish> wrapper = Wrappers.lambdaQuery(SetmealDish.class)
                .eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(wrapper);

        //查询分类名称
        String categoryName = setMealMapper.selectCategoryNameById(setmeal.getCategoryId());

        //封装到VO
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        setmealVO.setCategoryName(categoryName);

        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {
        //更新套餐基本信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setMealMapper.updateById(setmeal);

        //获取套餐id
        Long setmealId = setmealDTO.getId();

        //删除旧的套餐菜品关系数据
        LambdaQueryWrapper<SetmealDish> wrapper = Wrappers.lambdaQuery(SetmealDish.class)
                .eq(SetmealDish::getSetmealId, setmealId);
        setmealDishMapper.delete(wrapper);

        //插入新的套餐菜品关系数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmealId);
            }
            //批量插入套餐菜品关系数据
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDishMapper.insert(setmealDish);
            }
        }
    }

    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，需要判断套餐内是否有停售菜品，如果有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if (status == 1) {
            //查询套餐关联的菜品信息
            LambdaQueryWrapper<SetmealDish> wrapper = Wrappers.lambdaQuery(SetmealDish.class)
                    .eq(SetmealDish::getSetmealId, id);
            List<SetmealDish> setmealDishes = setmealDishMapper.selectList(wrapper);

            //获取菜品id列表
            List<Long> dishIds = setmealDishes.stream()
                    .map(SetmealDish::getDishId)
                    .collect(Collectors.toList());

            //查询菜品信息
            LambdaQueryWrapper<Dish> dishWrapper = Wrappers.lambdaQuery(Dish.class)
                    .in(Dish::getId, dishIds);
            List<Dish> dishes = dishMapper.selectList(dishWrapper);
            for (Dish dish : dishes) {
                if (dish.getStatus() == 0) {
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }

        //更新套餐状态
        Setmeal setmeal = new Setmeal();
        setmeal.setId(id);
        setmeal.setStatus(status);
        setMealMapper.updateById(setmeal);
    }
}
