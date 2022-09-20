package com.xxxx.seckill.controller;

import com.xxxx.seckill.mapper.GoodsMapper;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IUserService;
import com.xxxx.seckill.vo.DetailVo;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

/**
 * 商品
 * 乐字节：专注线上IT培训
 */
@Controller
@RequestMapping("/goods")
public class GoodsController {

	@Autowired
	private IUserService userService;
	@Autowired
	private IGoodsService goodsService;
	// ----页面缓存后后的改进---------
	@Autowired
	private RedisTemplate redisTemplate;                     // 引用redis缓存页面
	@Autowired
	private ThymeleafViewResolver thymeleafViewResolver;     // 手动渲染前端页面，视图解析器

//----------------------------------------------页面缓存前---------------------------------------------
//	/**
//	 * 功能描述: 跳转商品列表页
//	 * 先用session存用户信息，转为用redis存信息
//	 * 再简化判断用户是否存在的实现
//	 * 线程4000  轮数10
//	 * windows优化前QPS：1735.1822664372369/sec
//	 * Linux优化前QPS：1535.8427297044784/sec
//	 */
//	@RequestMapping("/toList")
//////1.	public String toList( HttpSession session, HttpServletResponse response, Model model, @CookieValue("userTicket") String ticket){  //存进session @CookieValue注解拿取名为("userTicket")的cookie
////2.	public String toList( HttpServletRequest request, HttpServletResponse response, Model model, @CookieValue("userTicket") String ticket){ //存进Redis
//	public String toList(Model model, User user){   // 去掉每次都判断用户是否登录 ，用拦截器配置 WebConfig 和 UserArgumentResolver
////2.		if (StringUtils.isEmpty(ticket)) {
////2.			return "login";
////2.		}
//////1.		User user = (User) session.getAttribute(ticket);                 // session里获取用户
////2.		User user = userService.getUserByCookie(ticket, request, response);
////2.		if (null == user) {
////2.			return "login";
////2.		}
//		model.addAttribute("user", user);                                        // 把用户对象传给前端
//		model.addAttribute("goodsList", goodsService.findGoodsVo());             // 添加商品列表信息，GoodsVo中的值
//		return "goodsList";
//	}
//
//	/**
//	 * 功能描述: 跳转商品详情页
//	 */
//	@RequestMapping("/toDetail/{goodsId}")
//	public String toDetail(Model model,User user, @PathVariable Long goodsId) {  // @PathVariable 映射 URL 绑定的占位符
//		model.addAttribute("user", user);
//		// 与当前事件作比较，判断是否到了秒杀时间
//		GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
//		Date startDate = goodsVo.getStartDate();
//		Date endDate = goodsVo.getEndDate();
//		Date nowDate = new Date();
//		//秒杀状态
//		int secKillStatus = 0;
//		//秒杀倒计时
//		int remainSeconds = 0;
//		//秒杀还未开始
//		if (nowDate.before(startDate)) {
//			remainSeconds = ((int) ((startDate.getTime() - nowDate.getTime()) / 1000)); //毫秒
//		} else if (nowDate.after(endDate)) {
//			//	秒杀已结束
//			secKillStatus = 2;
//			remainSeconds = -1;
//		} else {
//			//秒杀中
//			secKillStatus = 1;
//			remainSeconds = 0;
//		}
////		model.addAttribute("goods", goodsService.findGoodsVoByGoodsId(goodsId));  不加秒杀，单纯返回
//		model.addAttribute("remainSeconds", remainSeconds);
//		model.addAttribute("secKillStatus", secKillStatus);
//		model.addAttribute("goods", goodsVo);
//		return "goodsDetail";
//	}

//  --------------------------------------页面缓存后---------------------------------------------
	/**
	 * 功能描述: 跳转商品列表页（有缓存形式）
	 * 线程4000  轮数10
	 * windows优化前QPS：1735.1822664372369 /sec
	 * 		  加了缓存后QPS：3355.892387717434 /sec
	 * Linux优化前QPS：1535.8427297044784 /sec
	 */
	@RequestMapping(value = "/toList", produces = "text/html;charset=utf-8")  // 返回完整的页面，这个页面是缓存起来的
	@ResponseBody
	public String toList(Model model, User user,
	                     HttpServletRequest request, HttpServletResponse response) {
		//Redis中获取页面，如果不为空，直接返回页面
		ValueOperations valueOperations = redisTemplate.opsForValue();   // ValueOperations 这个包主要就是实现对单个值进行操作
		String html = (String) valueOperations.get("goodsList");         // 存的是html页面，就是String类型
		if (!StringUtils.isEmpty(html)) {
			return html;
		}
		model.addAttribute("user", user);
		model.addAttribute("goodsList", goodsService.findGoodsVo());
		// return "goodsList";
		//如果为空，手动渲染模板，不再通过 Thymeleaf，存入Redis并返回，把结果输出到浏览器端
		WebContext context = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());  // model.asMap()，把 model 转成 map，WebContext参数所需要的是map类型
		html = thymeleafViewResolver.getTemplateEngine().process("goodsList", context);
		if (!StringUtils.isEmpty(html)) {
			valueOperations.set("goodsList", html, 60, TimeUnit.SECONDS);   // 存入Redis，失效时间1分钟，
		}
		return html;
	}


//	/**
//	 * 功能描述: 跳转商品详情页（有缓存形式）
//	 * 不静态化
//	 */
//	@RequestMapping(value = "/toDetail/{goodsId}", produces = "text/html;charset=utf-8")   // URL 缓存，相当于页面缓存，把 goodsId 不一样的页面也缓存， 针对不同的详情页显示不同缓存页面，对不同的URL进行缓存
//	@ResponseBody
//	public String toDetail(Model model, User user, @PathVariable Long goodsId,
//	                        HttpServletRequest request, HttpServletResponse response) {
//		ValueOperations valueOperations = redisTemplate.opsForValue();
//		//Redis中获取页面，如果不为空，直接返回页面
//		String html = (String) valueOperations.get("goodsDetail:" + goodsId);
//		if (!StringUtils.isEmpty(html)) {
//			return html;
//		}
//		model.addAttribute("user", user);
//		GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
//		Date startDate = goodsVo.getStartDate();
//		Date endDate = goodsVo.getEndDate();
//		Date nowDate = new Date();
//		//秒杀状态
//		int secKillStatus = 0;
//		//秒杀倒计时
//		int remainSeconds = 0;
//		//秒杀还未开始
//		if (nowDate.before(startDate)) {
//			remainSeconds = ((int) ((startDate.getTime() - nowDate.getTime()) / 1000));
//		} else if (nowDate.after(endDate)) {
//			//	秒杀已结束
//			secKillStatus = 2;
//			remainSeconds = -1;
//		} else {
//			//秒杀中
//			secKillStatus = 1;
//			remainSeconds = 0;
//		}
//		model.addAttribute("remainSeconds", remainSeconds);
//		model.addAttribute("secKillStatus", secKillStatus);
//		model.addAttribute("goods", goodsVo);

//		WebContext context = new WebContext(request, response, request.getServletContext(), request.getLocale(),
//				model.asMap());
//		html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", context);
//		if (!StringUtils.isEmpty(html)) {
//			valueOperations.set("goodsDetail:" + goodsId, html, 60, TimeUnit.SECONDS);
//		}
//		// return "goodsDetail";
//		return html;
//	}

	/**
	 * 功能描述: 跳转商品详情页（有缓存形式）
	 * 静态化，返回的是RespBean
	 */
	@RequestMapping("/detail/{goodsId}")
	@ResponseBody
	public RespBean toDetail(User user, @PathVariable Long goodsId) {    // 将返回对象从 String 改为 RespBean
		GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
		Date startDate = goodsVo.getStartDate();
		Date endDate = goodsVo.getEndDate();
		Date nowDate = new Date();
		//秒杀状态
		int secKillStatus = 0;
		//秒杀倒计时
		int remainSeconds = 0;
		//秒杀还未开始
		if (nowDate.before(startDate)) {
			remainSeconds = ((int) ((startDate.getTime() - nowDate.getTime()) / 1000));
		} else if (nowDate.after(endDate)) {
			//	秒杀已结束
			secKillStatus = 2;
			remainSeconds = -1;
		} else {
			//秒杀中
			secKillStatus = 1;
			remainSeconds = 0;
		}

		//之前通过 model 传参，现在通过 DetailVo
		DetailVo detailVo = new DetailVo();
		detailVo.setUser(user);
		detailVo.setGoodsVo(goodsVo);
		detailVo.setSecKillStatus(secKillStatus);
		detailVo.setRemainSeconds(remainSeconds);
		return RespBean.success(detailVo);
	}
}