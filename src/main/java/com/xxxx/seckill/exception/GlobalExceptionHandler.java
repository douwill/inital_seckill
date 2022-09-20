package com.xxxx.seckill.exception;

import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理类 为了自定义注解@IsMobile可以在前端显示出错误信息
 */

// 处理控制器抛出的异常，用于定义@ExceptionHandler，@InitBinder和@ModelAttribute方法，适用于所有使用@RequestMapping的方法，会对所有@RequestMapping方法进行检查、拦截，并进行异常处理。
// 联合@ResponseBody是为了方便输出，使得这个GlobalExceptionHandler类里面的方法跟我们Controller类一样是输出的信息，返回值Result类型可以携带信息，当参数校验不通过的时候，输出也是Result（CodeMsg），传给前端用于前端显示获取处理
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)  //@ExceptionHandler可以用来统一处理方法抛出的异常，参数是某个异常类的class，代表这个方法专门处理该类异常，value=Exception.class代表拦截所有的异常
	public RespBean ExceptionHandler(Exception e) {
		if (e instanceof GlobalException) {  // 如果属于我们规定的全局异常
			GlobalException ex = (GlobalException) e;
			return RespBean.error(ex.getRespBeanEnum());
		} else if (e instanceof BindException) { // 如果属于绑定异常
			BindException ex = (BindException) e;
			RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
			respBean.setMessage("参数校验异常：" + ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
			return respBean;
		}
		return RespBean.error(RespBeanEnum.ERROR);
	}

}