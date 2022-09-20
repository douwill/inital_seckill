package com.xxxx.seckill.vo;


import com.xxxx.seckill.validator.IsMobile;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/**
 * 登录参数
 * 用来接收前端传递过来的参数
 * login.html中第80 81行数据
 */
@Data
public class LoginVo {
	@NotNull // 用validator组件进行参数校验就要规定为非空
	@IsMobile // 自定义注解，默认是true
	private String mobile;

	@NotNull
	@Length(min = 32) // 规定长度
	private String password;

}