package com.green.university.domain.counseling.controller;

import com.green.university.domain.counseling.dto.CounselingProfessorRequestDto;
import com.green.university.domain.counseling.dto.CounselingStudentRequestDto;
import com.green.university.domain.counseling.entity.CounselingReserve;
import com.green.university.domain.counseling.service.CounselingReserveService;
import com.green.university.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reserve")
@PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR')")
public class CounselingReserveController {

    private final CounselingReserveService counselingReserveService;

    public CounselingReserveController(CounselingReserveService counselingReserveService) {
        this.counselingReserveService = counselingReserveService;
    }

    // 로그인 유저 + requester에 따라 reserve db의 모든 내용을 가져오기
    @GetMapping("/list/requester")
    public ResponseEntity<?> getListByRequester(@AuthenticationPrincipal CustomUserDetails principal) {
        Long id = principal.getId();
        String userRole = principal.getUserRole();
        Map<String, List<CounselingReserve>> listByRequester = counselingReserveService.getListByRequester(id, userRole);
        return ResponseEntity.ok(listByRequester);
    }

    // 학생 상담 신청
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public void requestReserve(
            @Valid @RequestBody CounselingStudentRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        // 로그인한 사용자 ID 추출
        Long studentId = principal.getId();
        counselingReserveService.requestReserve(dto, studentId);
    }

    // 교수 승인 / 반려
    @PostMapping("/decision")
    @PreAuthorize("hasRole('PROFESSOR')")
    public void decideReserve(
            @RequestParam Long reserveId,
            @RequestParam String decision,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        // 교수 권한 체크는 SecurityConfig 또는 AOP에서 처리
        counselingReserveService.decideReserve(reserveId, decision);
    }

    // 학생 상담 예약 목록
    @GetMapping("/list")
    @PreAuthorize("hasRole('STUDENT')")
    public Object studentList(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long studentId = principal.getId();
        return counselingReserveService.getStudentReservationList(studentId);
    }



    // 학생: 교수가 보낸 상담 요청 requested, approved된 상담 개수
    @GetMapping("/count/student")
    @PreAuthorize("hasRole('STUDENT')")
    public java.util.Map<String, Integer> myCounts(@AuthenticationPrincipal CustomUserDetails principal) {
        Long studentId = principal.getId();
        return counselingReserveService.getMyCounts(studentId);
    }

    // 교수 -> 학생 상담요청
    @PostMapping("/pre/professor")
    @PreAuthorize("hasRole('PROFESSOR')")
    public void professorRequest(
            @Valid @RequestBody CounselingProfessorRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long professorId = principal.getId();
        counselingReserveService.professorRequest(dto, professorId);
    }

    // 학생: 내가 받은 교수 상담요청 목록
    @GetMapping("/pre/list/student")
    @PreAuthorize("hasRole('STUDENT')")
    public Object myPreList(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long studentId = principal.getId();
        return counselingReserveService.getMyPreReserveList(studentId);
    }

    // 학생: 수락 -> reserve 생성
    @PostMapping("/pre/accept")
    @PreAuthorize("hasRole('STUDENT')")
    public Object acceptPre(
            @RequestParam Long preReserveId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long studentId = principal.getId();
        Long reserveId = counselingReserveService.acceptPreReserve(studentId, preReserveId);
        return java.util.Map.of("reserveId", reserveId);
    }

    // 학생: 거절
    @PostMapping("/pre/reject")
    @PreAuthorize("hasRole('STUDENT')")
    public void rejectPre(
            @RequestParam Long preReserveId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long studentId = principal.getId();
        counselingReserveService.rejectPreReserve(studentId, preReserveId);
    }

    // 학생: 확정 상담 취소
    @DeleteMapping("/cancel/student")
    @PreAuthorize("hasRole('STUDENT')")
    public void cancelByStudent(@RequestParam Long reserveId,
                                @AuthenticationPrincipal CustomUserDetails principal) {
        counselingReserveService.cancelApprovedByStudent(principal.getId(), reserveId);
    }

    // 교수: 확정 상담 취소
    @DeleteMapping("/cancel/professor")
    @PreAuthorize("hasRole('PROFESSOR')")
    public void cancelByProfessor(@RequestParam Long reserveId,
                                  @AuthenticationPrincipal CustomUserDetails principal) {
        counselingReserveService.cancelApprovedByProfessor(principal.getId(), reserveId);
    }

    // 자신의 상담 방 코드 확인 (사용자 역할 별 구분, 룸 코드, 신청완료된 일정만, 시작시간 ~ 종료시간 사이만 입장가능)
    @GetMapping("/verify")
    public ResponseEntity<?> verifyRoomCode(@RequestParam("code") String roomCode, @AuthenticationPrincipal CustomUserDetails principal) {

        String room = roomCode.trim();

        boolean isValid;
        if(principal.getUserRole().equals("professor")) {
            Long professorId = principal.getId();
            isValid = counselingReserveService.isValidRoomPro(professorId, room);

        } else {
            Long studentId = principal.getId();
            isValid = counselingReserveService.isValidRoomStu(studentId,room);
        }

        return ResponseEntity.ok(isValid);

    }

    // 상담 진행 중 - 남은 시간 확인
    @GetMapping("/timeCheck")
    public ResponseEntity<?> timeCheck(@RequestParam("roomCode") String roomCode) {

        return ResponseEntity.ok(Map.of("endAt", counselingReserveService.getEndAtEpoch(roomCode)));
    }
}


