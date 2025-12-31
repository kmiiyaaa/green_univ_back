package com.green.university.infra.chatbot.handler;

import com.green.university.global.websocket.ChatHandler;
import com.green.university.infra.chatbot.intent.ChatIntent;
import com.green.university.infra.chatbot.intent.RouteMode;
import org.springframework.stereotype.Component;
import com.green.university.infra.chatbot.handler.*;

@Component
public class HandlerRegistry {
    private final PortalHandler portalHandler;
    private final OutOfScopeHandler outOfScopeHandler;
    private final QaHandler qaHandler;
    private final ClarifyHandler clarifyHandler;

    public HandlerRegistry(
            PortalHandler portalHandler,
            OutOfScopeHandler outOfScopeHandler,
            QaHandler qaHandler,
            ClarifyHandler clarifyHandler
    ) {
        this.portalHandler = portalHandler;
        this.outOfScopeHandler = outOfScopeHandler;
        this.qaHandler = qaHandler;
        this.clarifyHandler = clarifyHandler;
    }

    public ChatHandler get(ChatIntent intent, RouteMode mode) {

        // 1) OUT_OF_SCOPE는 무조건 범위 밖 처리
        if (mode == RouteMode.OUT_OF_SCOPE || intent == ChatIntent.OUT_OF_SCOPE) return outOfScopeHandler;

        // 2) 애매하지만 범위 내: 선택지 버튼 제공
        if (mode == RouteMode.CLARIFY) return clarifyHandler;

        // 3) QA 모드: 링크 없이 설명형 답변
        if (mode == RouteMode.QA) return qaHandler;

        // 4) 그 외: NAVIGATE(포털 안내)
        return portalHandler;
    }
}
