package com.example.fireview.global.response;

import java.util.List;

/**
 * 판정 사유 목록을 화면에 내보낼 때 쓰는 보정.
 *
 * <p>Data 서버는 사유가 없으면 {@code "reasons": []} 를 그대로 준다. 근거 코드 배열에
 * 안내 문장을 섞으면 저장·집계할 때 코드와 문장을 다시 갈라내야 하므로, 빈 배열이
 * 맞다. 대신 화면에는 아무것도 안 뜨는 칸이 생기므로 **응답을 만드는 시점에만**
 * 안내 문구를 끼운다.
 *
 * <p>그래서 이 보정은 프론트에 나가는 DTO 에서만 쓴다. 엔티티에 저장할 때는
 * 쓰지 않는다. 저장까지 문장이 들어가면 나중에 "사유 없음" 과 "사유가 진짜 있음" 을
 * 구분할 수 없고, 사유 코드 집계에도 이 문장이 한 건으로 섞여 들어간다.
 */
public final class ReasonMessages {

    /** 사유가 비어 있을 때 화면에 대신 보여줄 문구 */
    public static final String NO_DETAIL = "추가로 표시할 세부 사유가 없습니다.";

    private ReasonMessages() {
    }

    /**
     * 비어 있으면 안내 문구 한 줄로 바꾼다.
     *
     * <p>null 과 빈 배열을 같게 다룬다. 둘을 구분해봐야 화면에서 할 일이 같고,
     * 호출부마다 null 검사를 또 쓰게 된다.
     *
     * @return 사유가 있으면 공백 항목만 걸러낸 원본, 없으면 {@link #NO_DETAIL} 한 건
     */
    public static List<String> orPlaceholder(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return List.of(NO_DETAIL);
        }
        List<String> cleaned = reasons.stream()
                .filter(r -> r != null && !r.isBlank())
                .toList();
        return cleaned.isEmpty() ? List.of(NO_DETAIL) : cleaned;
    }
}
