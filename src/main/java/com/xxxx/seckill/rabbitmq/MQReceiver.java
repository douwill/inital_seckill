package com.xxxx.seckill.rabbitmq;


import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.SeckillMessage;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.impl.OrderServiceImpl;
import com.xxxx.seckill.utils.JsonUtil;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 消息消费者
 **/
@Service
@Slf4j
public class MQReceiver {

    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderServiceImpl orderService;
    /**
     * 真正的下单操作  以防万一还是要两次判断
     */
    @RabbitListener(queues = "seckillQueue")
    public void receive(String message){
        log.info("接受的消息：" + message);
        SeckillMessage seckillMessage = JsonUtil.jsonStr2Object(message, SeckillMessage.class);  // 拿到秒杀消息
        Long goodId = seckillMessage.getGoodId();
        User user = seckillMessage.getUser();

        // 根据id获取商品对象，然后判断库存
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodId);
        if (goodsVo.getStockCount() < 1) {
            return;
        }

        // 判断是否重复抢购
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodId);
        if (seckillOrder != null) {
//            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
            return;
        }

        // 可以开始下单
        orderService.seckill(user, goodsVo);
    }
}
