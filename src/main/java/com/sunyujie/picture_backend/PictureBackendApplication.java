package com.sunyujie.picture_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sunyujie.picture_backend.mapper")
public class PictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureBackendApplication.class, args);
    }

}
