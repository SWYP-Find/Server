package com.swyp.picke.domain.battle.thumbnail;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenAI Images API 로 이미지 한 장을 생성한다.
 * 레퍼런스 이미지가 있으면 edits(이미지 + 프롬프트), 없으면 generations(프롬프트만)를 호출한다.
 */
@Slf4j
@Component
public class OpenAiImageClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    // 이미지 생성은 수십 초가 걸릴 수 있어 넉넉히 둔다.
    private static final int READ_TIMEOUT_MS = 180000;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.image.generations-url}")
    private String generationsUrl;

    @Value("${openai.image.edits-url}")
    private String editsUrl;

    @Value("${openai.image.model}")
    private String model;

    @Value("${openai.image.quality}")
    private String quality;

    @Value("${openai.image.size}")
    private String size;

    public byte[] generate(String prompt, List<ReferenceImage> references) {
        Map<?, ?> response = references.isEmpty()
                ? callGenerations(prompt)
                : callEdits(prompt, references);
        return decodeFirstImage(response);
    }

    private Map<?, ?> callGenerations(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "n", 1,
                "size", size,
                "quality", quality);

        return restClient().post()
                .uri(generationsUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
    }

    private Map<?, ?> callEdits(String prompt, List<ReferenceImage> references) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("model", model);
        builder.part("prompt", prompt);
        builder.part("n", "1");
        builder.part("size", size);
        builder.part("quality", quality);
        for (ReferenceImage reference : references) {
            builder.part("image[]", new NamedByteArrayResource(reference.bytes(), reference.fileName()))
                    .contentType(reference.mediaType());
        }

        return restClient().post()
                .uri(editsUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(Map.class);
    }

    private byte[] decodeFirstImage(Map<?, ?> response) {
        if (response == null || !(response.get("data") instanceof List<?> data) || data.isEmpty()) {
            throw new IllegalStateException("OpenAI 이미지 응답에 data 가 없습니다.");
        }
        Object b64 = ((Map<?, ?>) data.get(0)).get("b64_json");
        if (!(b64 instanceof String encoded) || encoded.isBlank()) {
            throw new IllegalStateException("OpenAI 이미지 응답에 b64_json 이 없습니다.");
        }
        return Base64.getDecoder().decode(encoded);
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return RestClient.builder().requestFactory(factory).build();
    }

    /** multipart 파트로 보낼 때 파일명이 있어야 OpenAI 가 파일로 인식한다. */
    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String fileName;

        NamedByteArrayResource(byte[] bytes, String fileName) {
            super(bytes);
            this.fileName = fileName;
        }

        @Override
        public String getFilename() {
            return fileName;
        }
    }
}
