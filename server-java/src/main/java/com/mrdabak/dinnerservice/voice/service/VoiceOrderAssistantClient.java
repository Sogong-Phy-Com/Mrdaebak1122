package com.mrdabak.dinnerservice.voice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrdabak.dinnerservice.voice.VoiceOrderException;
import com.mrdabak.dinnerservice.voice.client.VoiceAssistantResponse;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderSession;
import com.mrdabak.dinnerservice.voice.model.VoiceOrderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VoiceOrderAssistantClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceOrderAssistantClient.class);
    private static final Pattern JSON_BLOCK_PATTERN =
            Pattern.compile("order_state_json\\s*:?\\s*```json\\s*(\\{.*?})\\s*```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String modelName;

    public VoiceOrderAssistantClient(RestTemplate restTemplate,
                                     ObjectMapper objectMapper,
                                     @Value("${voice.ollama.base-url:http://localhost:11434}") String baseUrl,
                                     @Value("${voice.ollama.model:qwen2.5:latest}") String modelName) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
    }

    public VoiceAssistantResponse generateResponse(VoiceOrderSession session, String menuPromptBlock) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", modelName);
            payload.put("stream", false);
            payload.put("messages", buildMessages(session, menuPromptBlock));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/chat", new HttpEntity<>(payload, headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new VoiceOrderException("대화형 AI 응답이 올바르지 않습니다.");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode messageNode = root.path("message").path("content");
            if (messageNode.isMissingNode()) {
                throw new VoiceOrderException("대화형 AI 응답을 읽을 수 없습니다.");
            }

            String content = messageNode.asText();
            return parseContent(content);
        } catch (VoiceOrderException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("LLM 호출 실패", e);
            throw new VoiceOrderException("상담원 응답을 생성하지 못했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    private List<Map<String, String>> buildMessages(VoiceOrderSession session, String menuPromptBlock) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        String systemPrompt = buildSystemPrompt(session, menuPromptBlock);
        messages.add(Map.of("role", "system", "content", systemPrompt));
        session.getMessages().forEach(msg ->
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent())));
        return messages;
    }

    private String buildSystemPrompt(VoiceOrderSession session, String menuPromptBlock) throws Exception {
        String stateJson = objectMapper.writeValueAsString(session.getCurrentState());
        return """
                당신은 미스터 대박 디너 서비스의 한국어 음성 주문 상담원입니다.
                - 항상 존댓말을 사용하고 고객 이름(%s)으로 호칭하세요.
                - 다룰 수 있는 주제: 디너 설명, 추천, 주문 변경, 결제, 배달 안내.
                - 도메인 외 질문은 정중하게 거절하고 다시 미스터 대박 디너 이야기로 이끌어 주세요.
                - 주문 단계: (1) 디너 선택 -> (2) 서빙 스타일 -> (3) 구성/수량 조정 -> (4) 날짜/시간 -> (5) 주소/연락처 -> (6) 최종 확인.
                - 마지막 응답에서는 아래 형식을 반드시 지켜주세요:
                  assistant_message:
                  (고객에게 들려줄 멘트)

                  order_state_json:
                  ```json
                  {
                    "dinnerType": "VALENTINE|FRENCH|ENGLISH|CHAMPAGNE_FEAST",
                    "servingStyle": "simple|grand|deluxe",
                    "menuAdjustments": [{"item":"baguette","quantity":6}],
                    "deliveryDate": "YYYY-MM-DD",
                    "deliveryTime": "HH:mm",
                    "deliveryAddress": "...",
                    "contactPhone": "...",
                    "specialRequests": "...",
                    "readyForConfirmation": true|false,
                    "needsMoreInfo": ["deliveryAddress"],
                    "summary": "한 줄 요약"
                  }
                  ```
                - menuAdjustments.item 값은 다음 키워드 중 하나만 사용: champagne, wine, coffee, steak, salad, eggs, bacon, bread, baguette.
                - 샴페인 축제 디너는 그랜드/디럭스만 허용됩니다.

                [메뉴 카탈로그]
                %s

                [현재 주문 상태 JSON]
                %s
                """.formatted(session.getCustomerName(), menuPromptBlock, stateJson);
    }

    private VoiceAssistantResponse parseContent(String content) {
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(content);
        VoiceOrderState state = new VoiceOrderState();
        String assistantMessage = content;
        if (matcher.find()) {
            String json = matcher.group(1);
            assistantMessage = content.substring(0, matcher.start())
                    .replace("assistant_message:", "")
                    .trim();
            try {
                state = objectMapper.readValue(json, VoiceOrderState.class);
            } catch (Exception e) {
                LOGGER.warn("주문 상태 JSON 파싱 실패: {}", e.getMessage());
            }
        } else {
            assistantMessage = assistantMessage.replace("assistant_message:", "").trim();
        }
        return new VoiceAssistantResponse(assistantMessage, state, content);
    }
}


