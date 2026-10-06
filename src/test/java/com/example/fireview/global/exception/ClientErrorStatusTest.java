package com.example.fireview.global.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 클라이언트 잘못이 500 으로 나가지 않는지 본다.
 *
 * <p>실제로 운영에서 아래가 전부 500 이었다. 맨 끝의 {@code Exception} 핸들러가 Spring 이
 * 상태 코드를 붙여 던진 예외까지 다 받아 500 으로 덮어썼다.
 * <pre>
 * GET    /api/landing/no-such-path              → 500 (404 여야 함)
 * GET    /api/products/abc                      → 500 (400 여야 함)
 * DELETE /api/landing/stats                     → 500 (405 여야 함)
 * </pre>
 * 프론트는 이걸 서버 장애로 읽었다.
 */
class ClientErrorStatusTest {

    @RestController
    @RequestMapping("/t")
    static class SampleController {
        @GetMapping("/items/{id}")
        String item(@PathVariable Long id) { return "ok"; }

        @GetMapping("/search")
        String search(@RequestParam String q) { return q; }

        @GetMapping("/boom")
        String boom() { throw new IllegalStateException("진짜 서버 오류"); }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 없는_경로는_404() throws Exception {
        mvc.perform(get("/t/no-such-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void 운영에서_나는_정적리소스_없음도_404() {
        // 운영은 리소스 핸들러가 있어 NoHandlerFound 대신 이 예외가 난다
        var res = new GlobalExceptionHandler()
                .handleNoResource(new NoResourceFoundException(HttpMethod.GET, "api/landing/no-such-path"));

        assertThat(res.getStatusCode().value()).isEqualTo(404);
        assertThat(res.getBody().errorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void 메서드가_틀리면_405와_Allow_헤더() throws Exception {
        mvc.perform(delete("/t/items/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")))
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 경로변수_타입이_틀리면_400() throws Exception {
        mvc.perform(get("/t/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'id' 값이 올바르지 않습니다."));
    }

    @Test
    void 필수_파라미터가_없으면_400() throws Exception {
        mvc.perform(get("/t/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'q' 파라미터가 필요합니다."));
    }

    @Test
    void 진짜_서버_오류는_여전히_500() throws Exception {
        // 클라이언트 오류만 걸러야 한다. 서버 버그까지 4xx 로 숨기면 장애를 못 본다
        mvc.perform(get("/t/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }
}
