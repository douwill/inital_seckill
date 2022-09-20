package com.xxxx.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxxx.seckill.pojo.Goods;
import com.xxxx.seckill.vo.GoodsVo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author qr
 * @since 2022-08-05
 */

public interface GoodsMapper extends BaseMapper<Goods> {
    /**
     *  获取商品列表
     */
    List<GoodsVo> findGoodsVo();

    /**
     * 功能描述: 获取商品详情
     */
    GoodsVo findGoodsVoByGoodsId(Long goodsId);

}
