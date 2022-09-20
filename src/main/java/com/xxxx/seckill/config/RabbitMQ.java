package com.xxxx.seckill.config;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * RabbitMQ配置类
 **/
@Configuration
public class RabbitMQ {

    // Fanout广播模式  定义队列和交换机
    private static final String QUEUE01 = "queue_fanout01";
    private static final String QUEUE02 = "queue_fanout02";
    private static final String EXCHANGE = "fanoutExchange";

    //  Direct完全匹配模式
//    private static final String ROUTINGKEY01 = "queue.red";
//    private static final String ROUTINGKEY02 = "queue.green";

    //  Topic模式，类似模糊查询
//    private static final String ROUTINGKEY01 = "#.queue.#";   * 代表一个单词    # 代表0个或多个单词
//    private static final String ROUTINGKEY02 = "*.queue.#";

    @Bean
    public Queue queue(){
        return new Queue("queue",true);
    }

    @Bean
    public Queue queue01(){
        return new Queue(QUEUE01);
    }

    @Bean
    public Queue queue02(){
        return new Queue(QUEUE02);
    }



    // 交换机
    @Bean
    public FanoutExchange fanoutExchange(){
    return new FanoutExchange(EXCHANGE);
    }

    // 将队列绑定到交换机
    @Bean
    public Binding binding01(){
    return BindingBuilder.bind(queue01()).to(fanoutExchange());
    }
    @Bean
    public Binding binding02(){
    return BindingBuilder.bind(queue02()).to(fanoutExchange());
    }

    // 带bindingkey的交换机
//    @Bean
//    public Binding binding01(){
//        return BindingBuilder.bind(queue01()).to(fanoutExchange()).with(ROUTINGKEY01);
//    }


}