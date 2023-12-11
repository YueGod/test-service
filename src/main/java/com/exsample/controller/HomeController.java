package com.exsample.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 *
 * @author YueGod
 * @since 2023/12/11
 */
@RestController
public class HomeController {

	@RequestMapping
	public String hello() {
		return "hello world";
	}

}
