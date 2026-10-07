package com.example.fireview.domain.dataserver;

import com.example.fireview.domain.dataserver.dto.DataServerProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Data 서버 상품 응답 JSON 픽스처 ({@code src/test/resources/dataserver}).
 *
 * <p>review-data 의 {@code GET /api/v1/{platform}/products/{id}} 응답 모양을 그대로 옮겼다.
 * 모르는 필드도 일부러 섞어 두어 무시되는지 함께 본다.
 */
public final class DataServerFixtures {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private DataServerFixtures() {}

    public static DataServerProductResponse load(String name) {
        try (InputStream in = DataServerFixtures.class.getResourceAsStream("/dataserver/" + name)) {
            if (in == null) throw new IllegalArgumentException("픽스처 없음: " + name);
            return MAPPER.readValue(in, DataServerProductResponse.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
