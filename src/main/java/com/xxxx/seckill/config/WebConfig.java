package com.xxxx.seckill.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC配置类，大于配置文件，就会按照配置类来找static静态资源，不处理就无法加载静态资源
 */
@Configuration  // @Configuration用于定义配置类
@EnableWebMvc   // @EnableWebMvc 将会使用本类的配置为mvc配置，完全覆盖默认配置，原本的一些默认配置并不生效，  会拦截静态资源！
public class WebConfig implements WebMvcConfigurer {

	@Autowired
	private UserArgumentResolver userArgumentResolver;                                     //优化登录用
	@Autowired
	private AccessLimitInterceptor accessLimitInterceptor;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {     //优化登录用
		resolvers.add(userArgumentResolver);
	}
//----------------------------------------以下是为了加载static静态资源-------------------------------------
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");  // 添加静态资源路径
	}
//-----------------------------------------------------------------------------------------------------
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(accessLimitInterceptor);
	}
}