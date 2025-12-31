package com.green.university.infra.chatbot.intent;

public enum RouteMode {
    NAVIGATE,   // 링크 안내 가능한 확실한 경우
    QA,         // 애매하면 AI 설명만
    CLARIFY,    // 애매하지만 범위 내: 선택지 버튼 제시
    OUT_OF_SCOPE
}
