package com.xxxx.seckill.controller;

import com.xxxx.seckill.service.IUserService;
import com.xxxx.seckill.vo.LoginVo;
import com.xxxx.seckill.vo.RespBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**

 */
@Controller
@RequestMapping("/login")
@Slf4j
public class LoginController {

	@Autowired
	private IUserService userService;

	/**
	 * 功能描述: 跳转登录页面
	 */
	@RequestMapping("/toLogin")
	public String toLogin(){
		return "login";
	}

	/**
	 * 功能描述: 登录功能
	 * @Valid 为入参的参数校验
	 */
	@RequestMapping("/doLogin")
	@ResponseBody  // 返回的是RespBean，所以用这个注解，使用此注解之后不会再走视图处理器，而是直接将数据写入到输入流中,将java对象转为json格式的数据。
	public RespBean doLogin(@Valid LoginVo loginVo, HttpServletRequest request, HttpServletResponse response){
		return userService.doLogin(loginVo,request,response);
	}

//	@RequestMapping("/doLogin")
//	@ResponseBody
//	public RespBean doLogin(LoginVo loginVo){  获取前端传进来的参数  login.html第80,81行数据
//		log.info("{}",loginVo);// 查看参数是否顺利接收
//		return null;
//	}

}