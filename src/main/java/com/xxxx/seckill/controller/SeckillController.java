package com.xxxx.seckill.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.tools.json.JSONUtil;
import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.SeckillMessage;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.rabbitmq.MQSender;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.service.ISeckillOrderService;
import com.xxxx.seckill.utils.JsonUtil;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  秒杀
 *  线程4000  轮数10
 *  windows优化前QPS：1557.0866908015105/sec
 *        缓存和静态化页面QPS：1286.904672536382/sec
 *  Linux优化前QPS：1042.698503727647/sec
 * </p>
 *
 * @author qr
 * @since 2022-08-05
 */
@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {   // 为了redis预减库存实现InitializingBean接口，为bean提供了初始化方法的方式，它只包括afterPropertiesSet方法，凡是继承该接口的类，在初始化bean的时候都会执行该方法。

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private ISeckillOrderService seckillOrderService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
    // 内存标记
    private Map<Long, Boolean> EmptyStockMap = new HashMap<>();  // Long指代不同的商品id

    /*
     * 实现InitializingBean接口，系统初始化的时候，把商品库存数量加载到Redis
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = goodsService.findGoodsVo();
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        list.forEach(goodsVo -> {
                    redisTemplate.opsForValue().set("seckillGoods:" + goodsVo.getId(), goodsVo.getStockCount());  // key为商品id  value为库存
                    EmptyStockMap.put(goodsVo.getId(), false);

                }
        );

    }

    // --------------------------------------------------秒杀非静态化-----------------------------------------
    /*
    @RequestMapping("/doSeckill")
    public String doSecKill(Model model, User user, Long goodsId){

          判断用户是否登录
        if(user == null){
            return "login";
        }
        model.addAttribute("user", user);
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);   // 不能读前端返回的库存，因为可以改动，要去数据库查询库存

        // 先进性两次抢购
        // 判断库存
        if (goods.getStockCount()<1){
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }

        // 判断是否重复抢购
        SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id", user.getId())
                .eq("goods_id", goodsId));                            // Mybatis_plus内容
        if (seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
            return "secKillFail";
        }

        // 都没有问题就生成订单
        Order order = orderService.seckill(user, goods);
        model.addAttribute("order",order);
        model.addAttribute("goods",goods);
        return "orderDetail";
    }
     */

// ---------------------------------------------------------秒杀静态化--------------------------------------------
    /*
    @RequestMapping(value="/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSecKill(User user, Long goodsId){
        if(user == null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
//        model.addAttribute("user", user);
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        // 先进行两次判断
        // 判断库存
        if (goods.getStockCount()<1){
//            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // 判断是否重复抢购
//        SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id", user.getId())
//                .eq("goods_id", goodsId));  // 改成下面用Redis获取
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (seckillOrder != null) {
//            model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }

        // 都没有问题就生成订单
        Order order = orderService.seckill(user, goods);
//        model.addAttribute("order",order);
//        model.addAttribute("goods",goods);
//        return "orderDetail";

        // 后端跳转改为了前端跳转
        return RespBean.success(order);  // 返回订单信息
        */

// ------------------------------------------------秒杀静态化后 用Redis预减库存，秒杀请求封装加入RabbitMQ队列---------------------------------
        @RequestMapping(value="/doSeckill", method = RequestMethod.POST)
        @ResponseBody
        public RespBean doSecKill(User user, Long goodsId){
            if(user == null){
                return RespBean.error(RespBeanEnum.SESSION_ERROR);
            }
            ValueOperations valueOperations = redisTemplate.opsForValue();

            // 判断是否重复抢购
            SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
            if (seckillOrder != null) {
                return RespBean.error(RespBeanEnum.REPEATE_ERROR);
            }

            // 获取内存标记，true为空 ，false为还有库存, 为true时直接返回错误，减少下面Redis的访问
            if (EmptyStockMap.get(goodsId)) {
                return RespBean.error(RespBeanEnum.EMPTY_STOCK);
            }
            // 预减库存
            Long stock = valueOperations.decrement("seckillGoods:" + goodsId);//递减，value为数字类型时，每调用一次就减1，并且具备原子性！！
            if (stock < 0) {                                                // 出现小于0的时候才进if，返回error
                EmptyStockMap.put(goodsId, true);                           // 加入内存标记
                valueOperations.increment("seckillGoods:" + goodsId);   // 当 stock=0 的时候还会执行使得 stock=-1，所以为了好看，递增加上1变成库存为0
                return RespBean.error(RespBeanEnum.EMPTY_STOCK);
            }

            // 消息队列
            // 以上都通过再下单
//            Order order = orderService.seckill(user, goods); // 改成在MQ中下单
            SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);   // 将秒杀信息封装成对象传入MQ队列

            mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));  // 将对象转为Json字符串，去 MQReceiver 中生成订单
            return RespBean.success(0);

        }

    /*
     * 轮询获取秒杀结果
     * 返回：orderId 成功， -1 秒杀失败， 0 排队中
     */
    @RequestMapping(value="/result", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user, Long goodsId){
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        Long orderId = seckillOrderService.getResult(user, goodsId);
        return RespBean.success(orderId);
    }

}

