package com.green.university;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class Teamproject2GreenUniApplication {



    public static void main(String[] args) {
        SpringApplication.run(Teamproject2GreenUniApplication.class, args);
    }

    // 한국시간
//    @PostConstruct
//    public void started() {
//        // 애플리케이션의 기본 시간대를 한국(KST)으로 설정
//        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
//    }

}
