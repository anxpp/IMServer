package com.anxpp.tinyim;

import com.anxpp.tinyim.server.ServerStarter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;

@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @Resource
    private ServerStarter serverStarter;

    //启动服务器
    @Bean
    CommandLineRunner start() {
        return args -> serverStarter.startup();
    }

}