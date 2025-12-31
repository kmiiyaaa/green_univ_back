package com.green.university.infra.chatbot.handler;

import com.green.university.global.websocket.ChatHandler;
import com.green.university.global.websocket.dto.ChatContext;
import com.green.university.infra.chatbot.dto.ChatResponseDto;
import com.green.university.infra.chatbot.util.RoleNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClarifyHandler implements ChatHandler {

    @Override
    public ChatResponseDto handle(ChatContext ctx) {
        String role = RoleNormalizer.normalize(ctx.getUserRole());
        String m = normalize(ctx.getMessage());

        // 기본 멘트
        String answer =
                "어떤 메뉴를 찾는지 조금 더 알려주세요 🙂\n" +
                        "아래에서 원하는 항목을 선택해 주세요!";

        // “학사”처럼 넓은 단어를 치면, staff도 '전부'가 아니라 '학사 관련'만 보여주자
        if (m.contains(normalize("학사")) && !m.contains(normalize("등록"))) {
            return clarifyForAcademic(role);
        }

        // 등록금/고지서는 등록으로 오인되기 쉬우니 먼저 처리
        // - 등록금 = 등록 + 금 이라서 아래 '등록' 분기로 빠지면 등록 메뉴가 다 나와버림
        if (m.contains(normalize("등록금")) || m.contains(normalize("고지서"))) {
            return clarifyForTuition(role);
        }

        // 등록을 치면 등록 관련만 묶어서 보여주자 (교직원 전용 UX)
        // “등록금”은 위에서 이미 처리했으니 제외
        if ((m.contains(normalize("등록")) && !m.contains(normalize("등록금")))
                || containsAny(m, List.of("유저등록", "사용자등록", "계정등록", "학생등록", "교수등록", "교직원등록"))) {
            return clarifyForRegister(role);
        }

        // 그 외: 최소 공통 + role별 자주 쓰는 것만
        return clarifyDefault(role, answer);
    }


    // 학사 관련 선택지
    private ChatResponseDto clarifyForAcademic(String role) {
        String answer =
                "학사 관련해서 어떤 걸 찾으세요? 🙂\n" +
                        "아래에서 선택해 주세요!";

        List<ChatResponseDto.Link> links = new ArrayList<>();

        // 공통(누구나)
        links.add(new ChatResponseDto.Link("학사일정 조회", "/schedule"));
        links.add(new ChatResponseDto.Link("공지사항", "/notice"));

        // 교직원은 학사 업무성 메뉴도 함께
        if ("staff".equals(role)) {
            links.add(new ChatResponseDto.Link("학사일정 관리/등록", "/schedule/write"));
            links.add(new ChatResponseDto.Link("수강신청 기간 설정", "/sugang/period"));
            links.add(new ChatResponseDto.Link("휴학 신청 처리", "/break/list/staff")); // 학사 범주에 자주 포함됨
        }

        // 학생/교수는 학사 범주에서 자주 묻는 것만 살짝 얹기(원하면 삭제 가능)
        if ("student".equals(role)) {
            links.add(new ChatResponseDto.Link("수강신청", "/sugang"));
        }

        return new ChatResponseDto(answer, links, List.of());
    }


    // 등록금 관련
    private ChatResponseDto clarifyForTuition(String role) {
        String answer =
                "등록금 관련해서 어떤 걸 찾으세요? 🙂\n" +
                        "아래에서 선택해 주세요!";

        List<ChatResponseDto.Link> links = new ArrayList<>();

        // 교직원: 등록금 고지서 생성/발송 + 단대별 등록금 조회/등록(관리)
        if ("staff".equals(role)) {
            links.add(new ChatResponseDto.Link("등록금 고지서 생성/발송", "/tuition/bill"));

            // 단대 등록금 조회/등록은 보통 /admin/colltuit 한 페이지에서 관리(너 코드 기준)
            links.add(new ChatResponseDto.Link("단대별 등록금 조회/등록", "/admin/colltuit"));

            // 필요하면 나중에 실제 페이지가 있을 때만 추가해도 됨
            // links.add(new ChatResponseDto.Link("등록금 발송 내역/현황", "/tuition/bill/list"));
        } else {
            // 학생: 고지서 조회/납부 + 납부 내역
            // (교수/게스트가 등록금 문의해도 학생용 안내 정도는 무방)
            links.add(new ChatResponseDto.Link("등록금 고지서 조회/납부", "/tuition/payment"));
            links.add(new ChatResponseDto.Link("등록금 납부 내역", "/tuition"));
        }

        return new ChatResponseDto(answer, links, List.of());
    }


    // 등록 관련
    private ChatResponseDto clarifyForRegister(String role) {

        // 교직원 아니면 등록 메뉴는 권한상 의미 없으니 최소화
        if (!"staff".equals(role)) {
            return new ChatResponseDto(
                    "등록/관리 기능은 교직원 계정에서 이용할 수 있어요 🙂\n" +
                            "대신 아래에서 필요한 항목을 확인해 주세요!",
                    List.of(
                            new ChatResponseDto.Link("공지사항", "/notice"),
                            new ChatResponseDto.Link("학사일정 조회", "/schedule")
                    ),
                    List.of()
            );
        }

        // staff 등록 관련
        return new ChatResponseDto(
                "어떤 항목을 등록하시겠어요? 아래에서 선택해 주세요 🙂",
                List.of(
                        new ChatResponseDto.Link("단과대 등록", "/admin/college"),
                        new ChatResponseDto.Link("학과 등록", "/admin/department"),
                        new ChatResponseDto.Link("강의실 등록", "/admin/room"),
                        new ChatResponseDto.Link("강의 등록", "/admin/subject"),
                        new ChatResponseDto.Link("단대별 등록금 등록", "/admin/colltuit"),
                        new ChatResponseDto.Link("사용자 등록(학생/교수/교직원)", "/user/create")
                ),
                List.of()
        );
    }



    // =========================
    // 3) 기본(default) 선택지
    // =========================
    private ChatResponseDto clarifyDefault(String role, String answer) {
        List<ChatResponseDto.Link> links = new ArrayList<>();

        // 공통(누구나)
        links.add(new ChatResponseDto.Link("학사일정 조회", "/schedule"));
        links.add(new ChatResponseDto.Link("공지사항", "/notice"));

        // role별 최소 추천만
        if ("staff".equals(role)) {
            links.add(new ChatResponseDto.Link("수강신청 기간 설정", "/sugang/period"));
            links.add(new ChatResponseDto.Link("사용자 등록(학생/교수/교직원)", "/user/create"));
        }

        if ("student".equals(role)) {
            links.add(new ChatResponseDto.Link("수강신청", "/sugang"));
            links.add(new ChatResponseDto.Link("성적 조회(금학기)", "/grade/current"));
        }

        if ("professor".equals(role)) {
            links.add(new ChatResponseDto.Link("내 강의 조회", "/professor/subject"));
            links.add(new ChatResponseDto.Link("상담 관리", "/counseling/manage"));
        }

        return new ChatResponseDto(answer, links, List.of());
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replace(" ", "");
    }

    private boolean containsAny(String normalizedMessage, List<String> keywords) {
        for (String k : keywords) {
            if (normalizedMessage.contains(normalize(k))) return true;
        }
        return false;
    }
}
