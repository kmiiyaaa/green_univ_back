package com.green.university;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.TimeZone;

@SpringBootTest
class Teamproject2GreenUniApplicationTests {

    @Test
    void contextLoads() {
    }

    // 한국시간
    @PostConstruct
    public void started() {
        // 애플리케이션의 기본 시간대를 한국(KST)으로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

}
