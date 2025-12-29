package com.green.university.domain.counseling.controller;

import com.green.university.domain.counseling.dto.CounselingScheduleDto;
import com.green.university.domain.counseling.dto.DeleteScheduleRequestDto;
import com.green.university.domain.counseling.dto.WeeklyCounselingScheduleRequest;
import com.green.university.domain.counseling.service.CounselingScheduleService;
import com.green.university.domain.counseling.service.RiskStudentService;
import com.green.university.global.exception.CustomRestfullException;
import com.green.university.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/counseling")
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR')")
public class CounselingScheduleController {

    @Autowired
    private CounselingScheduleService counselingScheduleService;

    @Autowired
    private RiskStudentService riskStudentService;


    // 학생 - 과목별 상담 일정 조회 (오늘 이후 + 예약 안 된 것만)
    @GetMapping("/schedule")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getScheduleBySubject(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @RequestParam Long subjectId) {
        return ResponseEntity.ok(counselingScheduleService.getSchedulesBySubject(subjectId));
    }

    // 교수 - 프론트에서 넘어온 날짜(week의 월요일) 기준으로 2주 + 예약 여부 상관없이 내 상담 일정 불러오기
    @GetMapping("/professor")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> getSchedule(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam LocalDate weekStartDate) {
        Long id = principal.getId();
        LocalDate weekEndDate = weekStartDate.plusDays(11); // 월~금 , 다음주 평일까지
        List<CounselingScheduleDto> list = counselingScheduleService.getSchedulesByWeek(id, weekStartDate, weekEndDate);
        System.out.println("list: " + list);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/professor") // 교수 - 내 상담 일정 등록
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> weeklyCounselingSchedule(@AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody WeeklyCounselingScheduleRequest weeklyDto) {
        Long professorId = principal.getId(); // 로그인 교수
        counselingScheduleService.createWeeklySchedule(professorId, weeklyDto);

        return ResponseEntity.ok().body("일정 등록이 완료되었습니다");
    }

    @DeleteMapping("/professor") // 교수 - 내 상담 일정 삭제
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> deleteSchedule(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody DeleteScheduleRequestDto req
    ) {
        counselingScheduleService.deleteSchedules(
                principal.getId(),
                req.getCounselingDate(),
                req.getStartTime()
        );
        return ResponseEntity.ok().build();
    }


    @GetMapping("/riskStu") // 교수 - 이번 학기 내 담당 위험 학생 조회 (과목 별)
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> getMyRiskStu(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null || !Objects.equals(principal.getUserRole(), "professor")) {
            throw new CustomRestfullException("권한이 없는 페이지입니다.", HttpStatus.UNAUTHORIZED);
        }
        Long professorId = principal.getId(); // 로그인 교수
        return ResponseEntity.ok(riskStudentService.getRiskStudents(professorId));
    }


    // 포탈 알림 용 - 교수
    // 오늘의 상담 개수 보기
    @GetMapping("/today")
    @PreAuthorize("hasRole('PROFESSOR')")
    public int getCounselingByDate(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long professorId = principal.getId();
        return counselingScheduleService.counselingNumByDate(professorId);
    }


}
