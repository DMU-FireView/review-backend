package com.example.fireview.global.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot 는 {@code .properties} 를 ISO-8859-1 로 읽는다. 값에 한글을 넣으면 깨진 채
 * 쓰인다 — 홈 자동 채우기 키워드가 이렇게 깨져 엉뚱한 상품을 검색했다(#187).
 * 주석의 한글은 읽히지 않으므로 괜찮다. 값만 본다.
 */
class PropertiesEncodingTest {

    @Test
    void properties_값에는_ASCII만_쓴다() throws IOException {
        List<String> offending = new ArrayList<>();
        try (Stream<Path> files = Files.list(Path.of("src/main/resources"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".properties")).toList()) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue;
                    if (!StandardCharsets.US_ASCII.newEncoder().canEncode(line)) {
                        offending.add(file.getFileName() + ":" + (i + 1) + "  " + line);
                    }
                }
            }
        }
        assertThat(offending)
                .as("한글 등 비 ASCII 값은 코드 상수나 환경변수로 옮길 것")
                .isEmpty();
    }
}
