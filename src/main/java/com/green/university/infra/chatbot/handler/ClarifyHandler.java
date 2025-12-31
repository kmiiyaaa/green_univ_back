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

        String answer =
                "어떤 메뉴를 찾는지 조금 더 알려주세요 🙂\n" +
                        "아래에서 원하는 항목을 선택해 주세요!";

        List<ChatResponseDto.Link> links = new ArrayList<>();

        // 공통(누구나)
        links.add(new ChatResponseDto.Link("학사일정 조회", "/schedule"));
        links.add(new ChatResponseDto.Link("공지사항", "/notice"));

        // 교직원 전용 선택지(실제 존재하는 라우트 위주로)
        if ("staff".equals(role)) {
            links.add(new ChatResponseDto.Link("수강신청 기간 설정", "/sugang/period"));

            // “학사 등록”이라고 말했을 때 보통 관리/등록 메뉴들을 묶어줌
            links.add(new ChatResponseDto.Link("단과대 등록", "/admin/college"));
            links.add(new ChatResponseDto.Link("학과 등록", "/admin/department"));
            links.add(new ChatResponseDto.Link("강의실 등록", "/admin/room"));
            links.add(new ChatResponseDto.Link("강의 등록", "/admin/subject"));
            links.add(new ChatResponseDto.Link("단대별 등록금 등록", "/admin/colltuit"));

            // 통합 유저등록 페이지(너가 말한 /user/create)
            links.add(new ChatResponseDto.Link("사용자 등록(학생/교수/교직원)", "/user/create"));
        }

        // 교수/학생이면 상담/수강/성적 같은 걸 얹어줄 수도 있음
        if ("student".equals(role)) {
            links.add(new ChatResponseDto.Link("수강신청", "/sugang"));
            links.add(new ChatResponseDto.Link("성적 조회(금학기)", "/grade/current"));
        }
        if ("professor".equals(role)) {
            links.add(new ChatResponseDto.Link("내 강의 조회", "/professor/subject"));
            links.add(new ChatResponseDto.Link("상담 관리", "/counseling/manage"));
        }

        if ("staff".equals(role) && m.contains("등록")) {
            return new ChatResponseDto(
                    "어떤 항목을 등록하시겠어요? 아래에서 선택해 주세요 🙂",
                    List.of(
                            new ChatResponseDto.Link("단과대 등록", "/admin/college"),
                            new ChatResponseDto.Link("학과 등록", "/admin/department"),
                            new ChatResponseDto.Link("강의실 등록", "/admin/room"),
                            new ChatResponseDto.Link("강의 등록", "/admin/subject"),
                            new ChatResponseDto.Link("단대별 등록금 등록", "/admin/colltuit"),
                            new ChatResponseDto.Link("사용자 등록(학생/교수/교직원)", "/user/create") // ✅ 추가
                    ),
                    List.of()
            );
        }

        // references는 없어도 됨(선택)
        return new ChatResponseDto(answer, links, List.of());
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replace(" ", "");
    }
}
