package com.green.university.infra.chatbot.handler;

import com.green.university.global.websocket.ChatHandler;
import com.green.university.global.websocket.dto.ChatContext;
import com.green.university.infra.chatbot.dto.ChatResponseDto;
import com.green.university.infra.chatbot.intent.ChatIntent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PortalHandler implements ChatHandler {

    private final PortalCatalog catalog;

    public PortalHandler(PortalCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public ChatResponseDto handle(ChatContext ctx) {
        ChatIntent intent = ctx.getRouted().getIntent();

        Map<ChatIntent, PortalCatalog.Topic> topics = catalog.topics();
        PortalCatalog.Topic topic = topics.get(intent);

        if (topic == null) {
            // 매핑이 없으면 범위 밖 처리(안전)
            return new ChatResponseDto(
                    "해당 기능은 현재 챗봇이 안내할 수 없어요 😅\n포털 메뉴에서 다른 항목을 확인해 주세요!",
                    List.of(),
                    List.of()
            );
        }

        // "한 Topic = 한 intent"라서
        // 질문이 “학사일정”이면 “학사일정”만 버튼으로 내려감
//        String answer =
//                topic.title() + " 😊\n\n" +
//                        "아래 메뉴에서 확인할 수 있어요.\n" +
//                        "원하시면 아래 ‘바로가기’ 버튼을 눌러 이동해 주세요!";

        String pathBlock = "";
        if (topic.references() != null && !topic.references().isEmpty()) {
            pathBlock = "\n\n📌 경로\n- " + String.join("\n- ", topic.references());
        }

        String answer =
                topic.title() + " 😊\n\n" +
                        "아래 메뉴에서 확인할 수 있어요.\n" +
                        "원하시면 아래 ‘바로가기’ 버튼을 눌러 이동해 주세요!" +
                        pathBlock;

        return new ChatResponseDto(answer, topic.links(), topic.references());
    }
}
