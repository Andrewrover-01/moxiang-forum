package com.moxiang.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 墨香论坛 - Spring Boot Application Entry Point.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.moxiang")
@MapperScan("com.moxiang.mbg.mapper")
public class MoxiangApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoxiangApplication.class, args);
    }
}
