package com.green.university.infra.chatbot.handler;

import com.green.university.infra.chatbot.dto.ChatResponseDto;
import com.green.university.infra.chatbot.intent.ChatIntent;
import com.green.university.infra.chatbot.util.RoleNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PortalCatalog {

    /**
     * topic = 사용자 질문(intent)에 대해
     * 안내문구(title), 세부 안내(pageSummary), reference, 바로가기 링크, 키워드, 역할 목록
     */
    public record Topic(
            ChatIntent intent,
            String title,
            String pageSummary,            // 세부 안내용
            List<String> references,
            List<ChatResponseDto.Link> links,
            List<String> keywords,
            List<String> roles             // student,professor,staff
    ) {}

    // 한 번만 만들어서 재사용(캐싱)
    private final Map<ChatIntent, Topic> topics;

    public PortalCatalog() {
        this.topics = buildTopics();
    }

    public Map<ChatIntent, Topic> topics() {
        return topics;
    }

    /**
     * 라우터가 키워드 탐색할 때 사용할 “Topic 리스트”
     */
    public List<Topic> topicList() {
        return new ArrayList<>(topics.values());
    }

    /**
     * Mistral에게 보여줄 intent 후보 설명 문자열 생성
     * (너무 길면 분류가 흔들릴 수 있어서 title 정도만 간단히)
     */
    public String intentDescriptions() {
        StringBuilder sb = new StringBuilder();
        for (Topic t : topicList()) {
            sb.append("- ")
                    .append(t.intent().name())
                    .append(" (")
                    .append(t.title())
                    .append(")\n");
        }
        sb.append("- OUT_OF_SCOPE\n");
        sb.append("- UNKNOWN\n");
        return sb.toString();
    }


    //role 기반 필터링
    public List<Topic> topicListByRole(String role) {
        String r = RoleNormalizer.normalize(role);
        if (r == null || r.isBlank()) return topicList();
        return topicList().stream()
                .filter(t -> t.roles() == null || t.roles().isEmpty() || t.roles().contains(r))
                .toList();
    }

    /**
     * QA 모드에서 사용할 “요약 지식팩”
     * - role에 맞는 Topic만 모아서 pageSummary/경로를 한 번에 제공
     * - AI가 포털 범위 내에서만 안내하도록 제한할 때 사용
     */
    public String buildSummaryPackForRole(String role) {
        StringBuilder sb = new StringBuilder();

        for (Topic t : topicListByRole(role)) {
            sb.append("[").append(t.intent().name()).append("] ").append(t.title()).append("\n");

            if (t.pageSummary() != null && !t.pageSummary().isBlank()) {
                sb.append("- 설명: ").append(t.pageSummary()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // =========================================================
    // 내부: 토픽 빌드
    // =========================================================
    private Map<ChatIntent, Topic> buildTopics() {

        List<Topic> list = List.of(
                // ===================== 성적(학생) =====================
                new Topic(
                        ChatIntent.GRADE_CURRENT,
                        "금학기 성적 조회 안내",
                        "금학기(현재 학기) 성적을 확인하는 메뉴입니다. 과목별 점수/등급을 확인할 수 있어요.",
                        List.of("포털 > 성적 > 금학기 성적 조회"),
                        List.of(new ChatResponseDto.Link("금학기 성적조회 바로가기", "/grade/current")),
                        List.of("금학기", "이번학기성적", "이번 학기 성적", "성적확인", "성적조회", "current grade"),
                        List.of("student")
                ),
                new Topic(
                        ChatIntent.GRADE_SEMESTER,
                        "학기별 성적 조회 안내",
                        "학기(년도/학기)를 선택해서 해당 학기의 성적을 확인할 수 있어요.",
                        List.of("포털 > 성적 > 학기별 성적 조회"),
                        List.of(new ChatResponseDto.Link("학기별 성적조회 바로가기", "/grade/semester")),
                        List.of("학기별성적", "학기 성적", "지난학기성적", "semester grade"),
                        List.of("student")
                ),
                new Topic(
                        ChatIntent.GRADE_TOTAL,
                        "누계 성적 조회 안내",
                        "전체 학기 누적 성적(GPA/평점)을 확인하는 메뉴입니다.",
                        List.of("포털 > 성적 > 누계 성적"),
                        List.of(new ChatResponseDto.Link("누계 성적 바로가기", "/grade/total")),
                        List.of("누계", "전체성적", "총성적", "평점", "gpa", "total grade"),
                        List.of("student")
                ),

                // ===================== 공지 / 학사일정(공용) =====================
                new Topic(
                        ChatIntent.NOTICE_LIST,
                        "공지사항 안내",
                        "학교 공지(학사/행사/안내 등)를 확인하는 메뉴입니다. 검색/카테고리로 빠르게 찾을 수 있어요.",
                        List.of("포털 > 학사 정보 > 공지사항"),
                        List.of(new ChatResponseDto.Link("공지사항 바로가기", "/notice")),
                        List.of("공지", "공지사항", "notice"),
                        List.of("student", "staff", "professor")
                ),
                new Topic(
                        ChatIntent.SCHEDULE_LIST,
                        "학사일정 안내",
                        "시험/방학/수강정정/등록기간 등 주요 학사일정을 확인하는 메뉴입니다.",
                        List.of("포털 > 학사 정보 > 학사일정"),
                        List.of(new ChatResponseDto.Link("학사일정 바로가기", "/schedule")),
                        List.of("학사일정", "학사 일정", "일정", "스케줄","학사", "schedule", "calendar"),
                        List.of("student", "staff", "professor")
                ),

                // ===================== 수업/수강신청 =====================
                new Topic(
                        ChatIntent.SUBJECT_LIST,
                        "전체 강의 조회 안내",
                        "개설된 전체 강의를 조회하는 메뉴입니다. (학과/학년/시간 등 조건으로 확인)",
                        List.of("포털 > 수업 > 전체 강의 조회"),
                        List.of(new ChatResponseDto.Link("전체 강의 조회 바로가기", "/subject/list")),
                        List.of("전체강의", "강의조회", "과목조회", "수업조회", "subject list"),
                        List.of("student", "professor")
                ),
                new Topic(
                        ChatIntent.SUGANG_LIST,
                        "강의 시간표 조회 안내",
                        "수강신청 전에 강의 시간표(개설 강의 일정)를 확인하는 메뉴입니다.",
                        List.of("포털 > 수강신청 > 강의 시간표 조회"),
                        List.of(new ChatResponseDto.Link("강의 시간표 조회 바로가기", "/sugang/list")),
                        List.of("강의시간표", "시간표조회", "강의 시간표", "timetable", "시간표"),
                        List.of("student")
                ),
                new Topic(
                        ChatIntent.SUGANG_PRE,
                        "예비 수강 신청 안내",
                        "예비 수강신청(장바구니/사전 담기 등)을 진행하는 메뉴입니다.",
                        List.of("포털 > 수강신청 > 예비 수강 신청"),
                        List.of(new ChatResponseDto.Link("예비 수강 신청 바로가기", "/sugang/pre")),
                        List.of("예비수강", "예비 수강", "pre sugang"),
                        List.of("student")
                ),
                new Topic(
                        ChatIntent.SUGANG_APPLY,
                        "수강 신청 안내",
                        "본 수강신청을 진행하는 메뉴입니다. 신청/취소/정원 등을 확인할 수 있어요.",
                        List.of("포털 > 수강신청 > 수강 신청"),
                        List.of(new ChatResponseDto.Link("수강 신청 바로가기", "/sugang")),
                        List.of("수강신청", "수강 신청", "신청", "enroll"),
                        List.of("student")
                ),
                new Topic(
                        ChatIntent.SUGANG_TIMETABLE,
                        "수강 신청 내역 조회 안내",
                        "내가 신청한 강의 목록(내 시간표)을 확인하는 메뉴입니다.",
                        List.of("포털 > 수강신청 > 수강 신청 내역 조회"),
                        List.of(new ChatResponseDto.Link("수강 신청 내역 바로가기", "/sugang/timetable")),
                        List.of("수강내역", "수강 신청 내역", "신청내역", "내 시간표", "최종시간표"),
                        List.of("student")
                ),
                new Topic(
                        ChatIntent.SUGANG_PERIOD,
                        "수강신청 기간 설정 안내(교직원)",
                        "교직원이 수강신청 기간(시작/종료)을 설정하는 메뉴입니다.",
                        List.of("포털 > 학사 관리 > 수강신청 기간 설정"),
                        List.of(new ChatResponseDto.Link("수강신청 기간 설정 바로가기", "/sugang/period")),
                        List.of("기간설정", "수강신청기간", "수강신청 기간 설정"),
                        List.of("staff")
                ),

                // ===================== 사용자(MY) =====================
                new Topic(
                        ChatIntent.USER_INFO,
                        "내 정보 조회 안내",
                        "내 개인정보/학적 정보 등을 확인하는 메뉴입니다.",
                        List.of("포털 > MY > 내 정보 조회"),
                        List.of(new ChatResponseDto.Link("내 정보 조회 바로가기", "/user/info")),
                        List.of("내정보", "내 정보", "회원정보", "user info", "프로필"),
                        List.of("student", "staff", "professor")
                ),
                new Topic(
                        ChatIntent.USER_PW,
                        "비밀번호 변경 안내",
                        "비밀번호를 변경하는 메뉴입니다. 보안상 주기적으로 변경을 권장합니다.",
                        List.of("포털 > MY > 비밀번호 변경"),
                        List.of(new ChatResponseDto.Link("비밀번호 변경 바로가기", "/user/update/password")),
                        List.of("비밀번호", "비번", "비밀번호변경", "password"),
                        List.of("student", "staff", "professor")
                ),

                // ===================== 휴학 =====================
                new Topic(
                        ChatIntent.BREAK_APP,
                        "휴학 신청 안내",
                        "휴학 신청서를 작성/제출하는 메뉴입니다. 사유/기간 등을 입력합니다.",
                        List.of("포털 > MY > 휴학 신청"),
                        List.of(new ChatResponseDto.Link("휴학 신청 바로가기", "/break/application")),
                        List.of("휴학신청", "휴학 신청", "휴학", "break application"),
                        List.of("student", "staff")
                ),
                new Topic(
                        ChatIntent.BREAK_LIST_STUDENT,
                        "휴학 내역 조회 안내(학생)",
                        "내 휴학 신청 내역/상태(승인/반려 등)를 확인하는 메뉴입니다.",
                        List.of("포털 > MY > 휴학 내역 조회"),
                        List.of(new ChatResponseDto.Link("휴학 내역 바로가기", "/break/list")),
                        List.of("휴학내역", "휴학 내역", "휴학조회", "휴학 신청 내역"),
                        List.of("student")
                ),
                new Topic(
                        ChatIntent.BREAK_LIST_STAFF,
                        "휴학 처리 안내(교직원)",
                        "교직원이 휴학 신청 리스트를 확인하고 승인/반려 처리하는 메뉴입니다.",
                        List.of("포털 > 학사 관리 > 휴학 처리"),
                        List.of(new ChatResponseDto.Link("휴학 처리 바로가기", "/break/list/staff")),
                        List.of("휴학처리", "휴학 처리", "휴학승인", "휴학 신청 리스트"),
                        List.of("staff")
                ),

                new Topic(
                        ChatIntent.COUNSELING_STATUS,
                        "내 학업 상태 안내",
                        "나의 학업 상태를 확인하는 메뉴입니다.",
                        List.of("포털 > 학업지원·상담 > 내 학업 상태"),
                        List.of(new ChatResponseDto.Link("내 학업 상태 바로가기", "/status")),
                        List.of("학업상태", "내 학업 상태", "상태", "status"),
                        List.of("student")
                ),
                new Topic(
                        ChatIntent.COUNSELING_MANAGE,
                        "상담 관리 안내",
                        "상담 신청/현황 확인 등 상담 관련 기능을 관리하는 메뉴입니다.",
                        List.of("포털 > 학업지원·상담 > 상담 관리"),
                        List.of(new ChatResponseDto.Link("상담 관리 바로가기", "/counseling/manage")),
                        List.of("상담관리", "상담 관리", "상담 신청", "상담 현황", "counseling manage"),
                        List.of("student", "professor")
                ),
                new Topic(
                        ChatIntent.COUNSELING_VIDEO,
                        "상담 바로가기 안내",
                        "예약된 상담이 있다면 이 메뉴에서 바로 상담(화상상담 등)으로 이동할 수 있어요.",
                        List.of("포털 > 학업지원·상담 > 상담 바로가기"),
                        List.of(new ChatResponseDto.Link("상담 바로가기", "/videotest")),
                        List.of("상담바로가기", "상담 바로가기", "화상상담", "video", "videotest"),
                        List.of("student", "professor")
                ),
                new Topic(
                        ChatIntent.PROFESSOR_COUNSELING_RISK,
                        "위험 학생 관리 안내",
                        "교수가 담당 학생의 위험 상태를 확인하고 상담 요청/관리할 수 있는 메뉴입니다.",
                        List.of("포털 > 모니터링·상담 > 위험 학생 관리"),
                        List.of(new ChatResponseDto.Link("위험 학생 관리 바로가기", "/professor/counseling/risk")),
                        List.of("위험학생", "위험 학생", "리스크", "risk student"),
                        List.of("professor")
                ),
                new Topic(
                        ChatIntent.PROFESSOR_COUNSELING_SCHEDULE,
                        "상담 시간 설정 안내",
                        "교수가 상담 가능 시간(스케줄)을 등록/수정하는 메뉴입니다.",
                        List.of("포털 > 모니터링·상담 > 상담 시간 설정"),
                        List.of(new ChatResponseDto.Link("상담 시간 설정 바로가기", "/professor/counseling/schedule")),
                        List.of("상담시간", "상담 시간 설정", "상담스케줄", "schedule counseling"),
                        List.of("professor")
                ),

                // ===================== 등록금 =====================
                new Topic(
                        ChatIntent.TUITION_LIST,
                        "등록금 내역 조회 안내",
                        "등록금 납부 내역을 확인하는 메뉴입니다.",
                        List.of("포털 > MY > 등록금 내역 조회"),
                        List.of(new ChatResponseDto.Link("등록금 내역 바로가기", "/tuition")),
                        List.of("등록금", "등록금내역", "납부내역", "tuition"),
                        List.of("student")
                ),
                new Topic(
                        ChatIntent.TUITION_PAYMENT,
                        "등록금 납부 고지서 안내",
                        "등록금 납부 고지서를 확인하는 메뉴입니다.",
                        List.of("포털 > MY > 등록금 납부 고지서"),
                        List.of(new ChatResponseDto.Link("등록금 고지서 바로가기", "/tuition/payment")),
                        List.of("고지서", "등록금고지서", "등록금 납부", "payment"),
                        List.of("student")
                ),
                new Topic(
                        ChatIntent.TUITION_BILL_CREATE,
                        "등록금 고지서 생성 안내(교직원)",
                        "교직원이 등록금 고지서를 생성/발송하는 메뉴입니다.",
                        List.of("포털 > 학사 관리 > 등록금 고지서 발송"),
                        List.of(new ChatResponseDto.Link("고지서 생성/발송 바로가기", "/tuition/bill")),
                        List.of("고지서발송", "고지서 생성", "등록금 고지서 발송"),
                        List.of("staff")
                ),

                // ===================== 명단 조회(교직원) =====================
                new Topic(
                        ChatIntent.PROFESSOR_LIST,
                        "교수 명단 조회 안내",
                        "교직원이 교수 명단을 조회하는 메뉴입니다.",
                        List.of("포털 > 학사 관리 > 교수 명단 조회"),
                        List.of(new ChatResponseDto.Link("교수 명단 조회 바로가기", "/professor/list")),
                        List.of("교수명단", "교수 리스트", "교수 조회"),
                        List.of("staff")
                ),
                new Topic(
                        ChatIntent.STUDENT_LIST,
                        "학생 명단 조회 안내",
                        "교직원이 학생 명단을 조회하는 메뉴입니다.",
                        List.of("포털 > 학사 관리 > 학생 명단 조회"),
                        List.of(new ChatResponseDto.Link("학생 명단 조회 바로가기", "/student/list")),
                        List.of("학생명단", "학생 리스트", "학생 조회"),
                        List.of("staff")
                ),

                // ===================== 교수 =====================
                new Topic(
                        ChatIntent.PROFESSOR_SUBJECT,
                        "내 강의 조회 안내(교수)",
                        "교수가 담당 강의를 조회하는 메뉴입니다.",
                        List.of("포털 > 수업 > 내 강의 조회"),
                        List.of(new ChatResponseDto.Link("내 강의 조회 바로가기", "/professor/subject")),
                        List.of("내강의", "내 강의", "교수강의", "professor subject"),
                        List.of("professor")
                ),
                new Topic(
                        ChatIntent.PROFESSOR_EVALUATION,
                        "내 강의 평가 안내(교수)",
                        "교수가 본인 강의의 평가를 확인하는 메뉴입니다.",
                        List.of("포털 > 수업 > 내 강의 평가"),
                        List.of(new ChatResponseDto.Link("내 강의 평가 바로가기", "/professor/evaluation")),
                        List.of("강의평가", "내 강의 평가", "evaluation"),
                        List.of("professor")
                ),

                // ===================== 관리자/등록(교직원) =====================
                new Topic(
                        ChatIntent.ADMIN_COLLEGE,
                        "단과대 등록 안내(관리자)",
                        "단과대(College) 정보를 등록/수정하는 메뉴입니다. 단과대명/코드 등의 정보를 관리합니다.",
                        List.of("포털 > 등록 > 단과 대학"),
                        List.of(new ChatResponseDto.Link("단과대 등록 바로가기", "/admin/college")),
                        List.of("단과대", "단과 대학", "college"),
                        List.of("staff")
                ),
                new Topic(
                        ChatIntent.ADMIN_DEPARTMENT,
                        "학과 등록 안내(관리자)",
                        "학과(Department) 정보를 등록/수정하는 메뉴입니다. 학과명/소속 단과대 등을 관리합니다.",
                        List.of("포털 > 등록 > 학과"),
                        List.of(new ChatResponseDto.Link("학과 등록 바로가기", "/admin/department")),
                        List.of("학과", "department"),
                        List.of("staff")
                ),
                new Topic(
                        ChatIntent.ADMIN_ROOM,
                        "강의실 등록 안내(관리자)",
                        "강의실(Room) 정보를 등록/수정하는 메뉴입니다. 강의실명/수용인원 등을 관리합니다.",
                        List.of("포털 > 등록 > 강의실"),
                        List.of(new ChatResponseDto.Link("강의실 등록 바로가기", "/admin/room")),
                        List.of("강의실", "room"),
                        List.of("staff")
                ),
                new Topic(
                        ChatIntent.ADMIN_SUBJECT,
                        "강의 등록 안내(관리자)",
                        "강의(Subject)를 등록/수정하는 메뉴입니다. 담당교수/시간/학점/정원 등을 설정합니다.",
                        List.of("포털 > 등록 > 강의"),
                        List.of(new ChatResponseDto.Link("강의 등록 바로가기", "/admin/subject")),
                        List.of("강의등록", "강의 등록", "subject create"),
                        List.of("staff")
                ),
                new Topic(
                        ChatIntent.ADMIN_COLLTUIT,
                        "단대별 등록금 등록 안내(관리자)",
                        "단과대별 등록금(CollTuit)을 등록/수정하는 메뉴입니다.",
                        List.of("포털 > 등록 > 단대별 등록금"),
                        List.of(new ChatResponseDto.Link("단대별 등록금 바로가기", "/admin/colltuit")),
                        List.of("단대등록금", "단대별등록금", "colltuit"),
                        List.of("staff")
                ),
                new Topic(
                        ChatIntent.USER_CREATE,
                        "유저 등록 안내(교직원)",
                        "학생/교수/교직원 계정을 등록하는 메뉴입니다.",
                        List.of("포털 > 학사 관리 > 유저 등록"),
                        List.of(new ChatResponseDto.Link("유저 등록 바로가기", "/user/create")),
                        List.of("유저등록","유저 등록","사용자등록","사용자 등록","계정등록","계정 등록","학생등록","교수등록","교직원등록"),
                        List.of("staff")
                )
        );

        // 리스트 -> EnumMap 변환 (성능/가독성 좋음)
        Map<ChatIntent, Topic> map = new EnumMap<>(ChatIntent.class);
        for (Topic t : list) {
            map.put(t.intent(), t);
        }
        return map;
    }
}
