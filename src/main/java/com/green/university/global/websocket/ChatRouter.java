package com.green.university.global.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.university.infra.chatbot.handler.PortalCatalog;
import com.green.university.infra.chatbot.intent.ChatIntent;
import com.green.university.infra.chatbot.intent.ChatRouteResult;
import com.green.university.infra.chatbot.intent.RouteMode;
import com.green.university.infra.chatbot.service.MistralClientService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ChatRouter {

    private final MistralClientService mistral;
    private final PortalCatalog catalog;
    private final ObjectMapper om = new ObjectMapper();

    public ChatRouter(MistralClientService mistral, PortalCatalog catalog) {
        this.mistral = mistral;
        this.catalog = catalog;
    }

    // 기존 route(String message)만 쓰는 구조였다면 여기서 오버로드로 받아도 됨
    public ChatRouteResult route(String message, String userRole) {
        String m = normalize(message);

        // 규칙(키워드) 기반 라우팅: 가장 정확하고 빠름
        // 긴 키워드 우선 매칭(“휴학 내역”이 “휴학”보다 우선)
        ChatIntent ruleHit = ruleMatchByCatalog(m);
        if (ruleHit != null) {
            ChatRouteResult r = new ChatRouteResult(ruleHit, "rule:catalog_keyword");
            r.setConfidence(1.0);
            r.setMode(RouteMode.NAVIGATE); // ✅ 규칙 hit면 무조건 NAVIGATE로
            return r;
        }

        // 2) 넓은 키워드는 OUT이 아니라 CLARIFY로 (가장 빠른 UX 개선)
        if (containsAny(m, List.of("학사", "등록", "유저등록", "사용자등록", "계정생성", "학생등록", "교수등록", "교직원등록"))) {
            ChatRouteResult r = new ChatRouteResult(ChatIntent.UNKNOWN, "ambiguous scope -> clarify");
            r.setConfidence(0.4);
            r.setMode(RouteMode.CLARIFY);
            return r;
        }

        // 3) Mistral에게 intent 분류 요청(애매한 케이스만)
        String systemPrompt =
                "너는 '그린대학교 포털' 안내 챗봇의 라우터다.\n" +
                        "사용자의 질문을 아래 intent 중 하나로만 분류해라.\n" +
                        "출력은 반드시 JSON 하나로만 반환해라. (json_object)\n\n" +
                        "intent 후보:\n" + catalog.intentDescriptions() + "\n" +
                        "JSON 출력 스키마:\n" +
                        "{ \"intent\": \"<INTENT>\", \"reason\": \"<짧은 근거>\" }\n\n" +
                        "규칙:\n" +
                        "- 포털 기능과 무관하면 intent=OUT_OF_SCOPE\n" +
                        "- 애매하면 OUT_OF_SCOPE\n" +
                        "- 이유는 짧게\n";

        String userPrompt = "사용자 질문: " + message;

        String json = mistral.classifyToJson(systemPrompt, userPrompt);

        // 4) JSON 파싱 -> ChatRouteResult
        try {
            JsonNode node = om.readTree(json);
            String intentStr = node.path("intent").asText("OUT_OF_SCOPE");
            String reason = node.path("reason").asText("");

            ChatIntent intent;
            try {
                intent = ChatIntent.valueOf(intentStr);
            } catch (Exception e) {
                intent = ChatIntent.OUT_OF_SCOPE;
            }

            if (intent == ChatIntent.OUT_OF_SCOPE) {
                ChatRouteResult r = new ChatRouteResult(ChatIntent.OUT_OF_SCOPE, reason);
                r.setConfidence(0.0);
                r.setMode(RouteMode.OUT_OF_SCOPE);
                return r;
            }

            ChatRouteResult r = new ChatRouteResult(intent, reason);
            r.setConfidence(0.75);
            r.setMode(RouteMode.QA);
            return r;

        } catch (Exception e) {
            // parse_fail도 OUT_OF_SCOPE로 처리하는 게 registry.get에서 안전
            ChatRouteResult r = new ChatRouteResult(ChatIntent.OUT_OF_SCOPE, "parse_fail");
            r.setConfidence(0.0);
            r.setMode(RouteMode.OUT_OF_SCOPE);
            return r;
        }
    }

    // catalog 에있는 topic 키워드로 intent 매칭
    //  "휴학 내역" > "휴학"처럼 긴 표현 우선
    private ChatIntent ruleMatchByCatalog(String normalizedMessage) {

        // 불변 리스트(List.of 등) 정렬하면 바로 터짐
        // 반드시 가변 리스트로 복사해서 정렬
        List<PortalCatalog.Topic> topics = new ArrayList<>(catalog.topicList());

        // Topic들을 키워드 최대 길이 기준으로 정렬(긴 표현 우선)
        topics.sort(
                Comparator.comparingInt((PortalCatalog.Topic t) ->
                        t.keywords().stream()
                                .mapToInt(k -> normalize(k).length())
                                .max()
                                .orElse(0)
                ).reversed()
        );

        // 휴학처럼 애매한 건 여기서 미리 가르는 게 안정적
        if (normalizedMessage.contains(normalize("휴학"))) {
            if (containsAny(normalizedMessage, List.of("내역", "조회", "신청내역"))) {
                return ChatIntent.BREAK_LIST_STUDENT;
            }
            if (containsAny(normalizedMessage, List.of("처리", "승인", "관리"))) {
                return ChatIntent.BREAK_LIST_STAFF;
            }
            return ChatIntent.BREAK_APP;
        }

        // 일반 키워드 탐색
        for (PortalCatalog.Topic t : topics) {
            for (String kw : t.keywords()) {
                if (normalizedMessage.contains(normalize(kw))) {
                    return t.intent();
                }
            }
        }
        return null;
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
