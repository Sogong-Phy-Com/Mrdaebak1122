package com.mrdabak.dinnerservice.voice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrdabak.dinnerservice.voice.VoiceOrderException;
import com.mrdabak.dinnerservice.voice.util.DomainVocabularyNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class SpeechToTextClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechToTextClient.class);

    private final RestTemplate restTemplate;
    private final DomainVocabularyNormalizer normalizer;
    private final String sttUrl;

    public SpeechToTextClient(RestTemplate restTemplate,
                              DomainVocabularyNormalizer normalizer,
                              @Value("${voice.stt.url:http://localhost:8001/stt/transcribe}") String sttUrl) {
        this.restTemplate = restTemplate;
        this.normalizer = normalizer;
        this.sttUrl = sttUrl;
    }

    public String transcribe(byte[] audioBytes, String filename) {
        try {
            ByteArrayResource resource = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            SttResponse response = restTemplate.postForObject(sttUrl, request, SttResponse.class);
            if (response == null || response.text == null) {
                throw new VoiceOrderException("음성 인식 결과를 가져오지 못했습니다.");
            }
            return normalizer.cleanupTranscript(response.text);
        } catch (VoiceOrderException e) {
            throw e;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                LOGGER.error("STT 서비스 연결 실패 - 서비스가 실행 중인지 확인하세요: {}", sttUrl, e);
                throw new VoiceOrderException("STT 서비스에 연결할 수 없습니다. STT 서비스가 실행 중인지 확인해주세요. (http://localhost:8001)", e);
            }
            LOGGER.error("STT 호출 실패 - 네트워크 오류", e);
            throw new VoiceOrderException("음성 인식 서비스에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.", e);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            LOGGER.error("STT 서비스 HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new VoiceOrderException("음성 인식 서비스 오류: " + e.getStatusCode(), e);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            LOGGER.error("STT 서비스 서버 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new VoiceOrderException("음성 인식 서비스 내부 오류가 발생했습니다. STT 서비스 로그를 확인해주세요.", e);
        } catch (Exception e) {
            LOGGER.error("STT 호출 실패: URL={}, Error={}", sttUrl, e.getMessage(), e);
            throw new VoiceOrderException("음성 인식에 실패했습니다. 다시 시도해주세요.", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SttResponse {
        @JsonProperty("text")
        private String text;
    }
}


