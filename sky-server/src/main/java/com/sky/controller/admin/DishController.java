package com.sky.controller.admin;

import com.github.pagehelper.Page;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Tag(name = "菜品管理")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    /**
     * 新增菜品
     * @return
     */
    @PostMapping("")
    @Operation(summary = "新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品: {}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("开始菜品分页查询: {}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     * @param ids 菜品id，多个id用逗号分隔，例如：1,2,3
     * @return
     */
    @DeleteMapping
    @Operation(summary = "批量删除菜品")
    public Result delete(@RequestParam("ids") String ids) {
        log.info("批量删除菜品: {}", ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改菜品信息
     * @param dishDTO
     * @return
     */
    @PutMapping("")
    @Operation(summary = "修改菜品信息")
    public Result update(@RequestBody DishDTO dishDTO) {
        //TODO: 还需要一个当前所改菜品的id, 需要改前端代码
        log.info("修改菜品信息: {}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 起售、停售菜品
     * @param status 状态：0停售 1起售
     * @param id 菜品id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "起售停售菜品")
    public Result startOrStop(@PathVariable("status") Integer status, Long id) {
        log.info("起售/停售菜品: status={}, id={}", status, id);
        dishService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("根据分类id查询菜品: categoryId={}", categoryId);
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    
}
