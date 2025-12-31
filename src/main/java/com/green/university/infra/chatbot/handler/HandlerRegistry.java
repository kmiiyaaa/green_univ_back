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


    public HandlerRegistry(PortalHandler portalHandler, OutOfScopeHandler outOfScopeHandler, QaHandler qaHandler, ClarifyHandler clarifyHandler ) {
        this.portalHandler = portalHandler;
        this.outOfScopeHandler = outOfScopeHandler;
        this.qaHandler = qaHandler;
        this.clarifyHandler = clarifyHandler;
    }

    public ChatHandler get(ChatIntent intent, RouteMode mode) {
        if (mode == RouteMode.OUT_OF_SCOPE || intent == ChatIntent.OUT_OF_SCOPE) return outOfScopeHandler;
        if (mode == RouteMode.QA) return qaHandler;
        return portalHandler;
    }
}
