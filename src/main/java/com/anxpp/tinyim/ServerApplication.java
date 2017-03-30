package com.anxpp.tinyim;

import com.anxpp.tinyim.server.ServerLauncherImpl;
import com.anxpp.tinyim.server.sdk.ServerLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    //启动服务器
    @Bean
    CommandLineRunner start() {
        return args -> {
            ServerLauncher serverLauncher = ServerLauncherImpl.getInstance();
            serverLauncher.startup();
        };
    }

}