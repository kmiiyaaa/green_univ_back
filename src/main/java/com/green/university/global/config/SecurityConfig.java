package com.green.university.global.config;

import com.green.university.global.security.JwtAuthenticationEntryPoint;
import com.green.university.global.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private JwtAuthenticationEntryPoint  jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                // logout 설정
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")   // 프론트에서 호출할 로그아웃 URL
                        .invalidateHttpSession(true)     // 세션 무효화(STATELESS지만 혹시 모를 세션 제거)
                        .clearAuthentication(true)       // SecurityContext 인증정보 제거
                        .logoutSuccessHandler((req, res, auth) ->
                                res.setStatus(HttpServletResponse.SC_OK))
                )
                .authorizeHttpRequests(auth -> auth
                // CORS preflight(OPTIONS) 전부 허용
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 에러, 정적 리소스, 로그인, 공지사항, 학사일정, 아이디 비밀번호 찾기 팝업 허용
                .requestMatchers(
                        "/",
                        "/error",
                        "/images/**",
                        "/api/auth/login",
                        "/api/notice/list/0",
                        "/api/schedule",
                        "/api/personal/find/id",
                        "/api/personal/find/password"
                ).permitAll()
                // 개발중이라 일단 다 열어두기
//                .requestMatchers("/**").permitAll()
                .anyRequest().authenticated()
        );

        // JWT 필터 등록 (UsernamePasswordAuthenticationFilter 앞에)
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 스프링 시큐리티에서 CORS(Cross-Origin Resource Sharing) 정책을 설정하는 코드
    // 스프링 시큐리티와 스프링 웹에서 "http://localhost:3000"에서 오는 다양한 HTTP 요청이 보안 정책(CORS) 때문에 막히지 않고 정상적으로 통과할 수 있도록 해주는 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        //config.setAllowedOrigins(List.of("http://localhost:5173")); // 추후 s3 bucket 주소도 추가해야함
        config.setAllowedOrigins(List.of("https://d28gjyhses04vx.cloudfront.net")); // cloudfront주소(프론트배포된)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        // 이 설정을 / 경로 이하 모든 요청에 적용하도록 등록하고 반환
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
