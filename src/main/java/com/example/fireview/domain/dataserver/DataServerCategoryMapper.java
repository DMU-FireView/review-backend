package com.example.fireview.domain.dataserver;

import com.example.fireview.domain.product.entity.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Data 서버 상품의 카테고리 문자열·상품명 → 내부 {@link Category}.
 *
 * <p>쇼핑몰마다 카테고리 체계가 다르고 자유 문자열이다.
 * <ul>
 *   <li>컬리 {@code "면/즉석식품/통조림 > 라면 > 봉지라면"}</li>
 *   <li>무신사 {@code "Digital/Tech > 음향가전 > 이어폰"}</li>
 *   <li>11번가 {@code "음향가전>이어폰>무선 이어폰"}</li>
 *   <li>올리브영 {@code "향수/디퓨저,맨즈에딧"} (검색 응답에는 이 넓은 값만 온다)</li>
 * </ul>
 * 컬리·무신사·11번가는 검색 응답에 카테고리가 없어서 상품명이 유일한 단서인 경우가 많다.
 *
 * <h3>순서</h3>
 * <ol>
 *   <li>카테고리 경로의 구체적인 칸부터 위로 — 쇼핑몰이 직접 붙인 분류가 가장 믿을 만하다</li>
 *   <li>상품명 — "히밥 티셔츠 (라면)" 같은 이름이 있어 경로보다 뒤에 본다</li>
 *   <li>넓은 분류("푸드", "홈리빙") — 대분류만 맞추는 마지막 수단</li>
 * </ol>
 * 규칙 목록은 "물건 종류"(의류·식기·가전)를 "맛·재료"(라면·커피)보다 앞에 둔다.
 * 그래야 "라면 그릇"이 식품이 아니라 주방용품이 된다.
 *
 * <p><b>모르면 null 이다.</b> 기타(ETC)로 넣으면 엉뚱한 분류가 진짜처럼 보인다.
 */
public final class DataServerCategoryMapper {

    private DataServerCategoryMapper() {}

    /** 의류인데 남녀·스포츠 구분은 문맥으로 정한다는 표시 */
    private static final Category APPAREL = null;

    private record Rule(Category category, List<String> keywords) {
        Rule(Category category, String... keywords) {
            this(category, Arrays.stream(keywords).map(DataServerCategoryMapper::normalize).toList());
        }

        boolean matches(String normalizedText) {
            return keywords.stream().anyMatch(normalizedText::contains);
        }
    }

    /**
     * 구체적인 규칙. 위에서부터 먼저 걸리는 것을 쓴다.
     * 짧은 낱말은 다른 낱말 속에 숨어 있어 피했다 ("칼"→칼국수, "컵"→컵라면, "크림"→아이스크림).
     */
    private static final List<Rule> SPECIFIC = List.of(
            // 대상이 분명한 것: 반려동물·유아
            new Rule(Category.PET_DOG, "강아지", "반려견", "애견", "사료"),
            new Rule(Category.PET_CAT, "고양이", "반려묘", "캣타워", "캣닢", "두부모래", "벤토나이트"),
            new Rule(Category.PET_OTHER, "햄스터", "어항", "열대어", "관상어", "앵무새"),
            new Rule(Category.BABY_NEWBORN, "기저귀", "분유", "젖병", "유모차", "카시트", "출산", "임산부"),
            new Rule(Category.BABY_CLOTHES, "아동복", "유아복", "아동의류", "유아동의류", "베이비의류", "키즈의류"),
            // "블록"은 선블록에 걸려서 쓰지 않는다
            new Rule(Category.BABY_TOY, "장난감", "완구", "교구", "레고", "보드게임", "인형"),

            // 디지털/가전
            new Rule(Category.DIGITAL_KITCHEN, "에어프라이어", "전자레인지", "밥솥", "믹서기", "블렌더",
                    "커피머신", "전기포트", "토스터", "오븐", "레인지후드", "주방가전"),
            new Rule(Category.DIGITAL_HOME_APPLIANCE, "청소기", "공기청정기", "선풍기", "서큘레이터", "제습기",
                    "가습기", "드라이기", "헤어드라이어", "생활가전"),
            new Rule(Category.DIGITAL_AV, "이어폰", "헤드폰", "헤드셋", "headphone", "earphone", "스피커",
                    "음향", "사운드바", "텔레비전", "프로젝터", "에어팟", "버즈"),
            // "컴퓨터"는 "컴퓨터 의자"에 걸려서 쓰지 않는다
            new Rule(Category.DIGITAL_PC, "노트북", "lg그램", "맥북", "데스크탑", "모니터", "키보드", "마우스",
                    "외장하드", "pc주변"),
            new Rule(Category.DIGITAL_MOBILE, "휴대폰", "스마트폰", "핸드폰", "아이폰", "z플립", "z폴드",
                    "태블릿", "아이패드", "스마트워치", "보조배터리", "폰케이스", "범퍼케이스"),

            // 자동차. "차량용 청소기"처럼 가전이 붙으면 가전이 맞다
            new Rule(Category.AUTO_CAR, "차량용", "자동차", "카매트", "블랙박스", "와이퍼", "세차", "엔진오일"),
            new Rule(Category.AUTO_MOTO, "오토바이"),
            // "공구"만으로는 안 된다. 상품명에서 공동구매([공구])로 자주 쓴다
            new Rule(Category.AUTO_TOOL, "전동공구", "수공구", "공구세트", "드릴", "작업복"),

            // 골프·향수는 낱말이 분명해서 패션보다 먼저 본다 ("골프티 Control Tee", "드레스룸 향수")
            new Rule(Category.SPORTS_GOLF, "골프"),
            new Rule(Category.LUXURY_BEAUTY, "향수", "edp", "edt"),

            // 패션. 신발이 먼저다 — "트레이닝화"가 운동복으로 가지 않게
            new Rule(Category.ACC_SHOES, "신발", "운동화", "스니커즈", "구두", "샌들", "부츠", "슬리퍼",
                    "로퍼", "러닝화", "등산화", "트레킹화", "워킹화", "슈즈", "shoes"),
            new Rule(Category.ACC_BAG, "가방", "백팩", "숄더백", "토트백", "크로스백", "에코백", "클러치", "캐리어"),
            new Rule(Category.FASHION_UNDERWEAR, "언더웨어", "속옷", "잠옷", "파자마", "홈웨어", "양말",
                    "브라렛", "브래지어", "팬티", "드로즈"),
            new Rule(Category.FASHION_SPORTS, "스포츠의류", "스포츠웨어", "sportswear", "트레이닝", "레깅스",
                    "수영복", "래쉬가드", "요가복", "운동복"),
            // "벨트"만으로는 안전벨트·하이체어에 걸린다
            new Rule(Category.ACC_WALLET, "지갑", "wallet", "월렛", "카드케이스", "가죽벨트", "벨트버클",
                    "남성벨트", "여성벨트"),
            new Rule(Category.FURNITURE_DECO, "벽시계"),
            // "비니"는 비니거(식초)에 걸려서 비니모자로 쓴다
            new Rule(Category.ACC_ACCESSORY, "모자", "볼캡", "스냅백", "비니모자", "보닛", "bonnet", "버킷햇",
                    "플로피햇", "시계", "선글라스", "안경테", "주얼리", "목걸이", "귀걸이", "반지", "팔찌",
                    "머플러", "스카프", "헤어핀"),
            // "저지"는 저지우유에, "의류"는 "의류용 세제"에 걸려서 쓰지 않는다
            new Rule(APPAREL, "티셔츠", "셔츠", "니트", "원피스", "드레스", "스커트", "치마", "바지", "팬츠",
                    "청바지", "데님", "자켓", "재킷", "코트", "패딩", "점퍼", "아우터", "후드", "맨투맨",
                    "블라우스", "가디건", "슬랙스", "져지", "상의", "하의", "clothing",
                    "tee", "shirt", "longsleeve", "dress", "knit", "pants", "denim", "hoodie", "jacket", "skirt"),

            // 스포츠는 "캠핑 의자"가 가구로, "요가 매트 타월"이 생활용품으로 가지 않게 먼저 본다
            new Rule(Category.SPORTS_OUTDOOR, "캠핑", "텐트", "침낭", "등산", "아웃도어", "타프", "코펠"),
            new Rule(Category.SPORTS_BIKE, "자전거", "킥보드", "스케이트보드", "인라인"),
            new Rule(Category.SPORTS_FITNESS, "요가", "필라테스", "덤벨", "아령", "운동기구", "폼롤러", "짐볼"),

            // 생활/주방
            new Rule(Category.LIVING_KITCHEN, "프라이팬", "후라이팬", "그릴팬", "웍팬", "냄비", "뚝배기", "식기",
                    "그릇", "접시", "머그", "텀블러", "수저", "젓가락", "숟가락", "도마", "밀폐용기", "보관용기",
                    "주방용품", "커피잔", "유리컵"),
            new Rule(Category.LIVING_STORAGE, "수납", "정리함", "리빙박스", "옷걸이", "선반", "압축팩", "행거"),
            new Rule(Category.LIVING_SAFETY, "멀티탭", "방범", "소화기", "구급"),
            new Rule(Category.LIVING_DAILY, "세제", "세정제", "섬유유연제", "휴지", "화장지", "물티슈", "청소용품",
                    "욕실", "수건", "타월", "타올", "칫솔", "치약", "구강", "생리대", "위생용품",
                    "덴탈마스크", "보건용마스크", "kf94"),

            // 가구/인테리어. 조명이 먼저다 — "책상 스탠드 조명"이 가구로 가지 않게.
            new Rule(Category.FURNITURE_BEDDING, "이불", "베개", "매트리스", "토퍼", "침구", "담요", "듀벳", "duvet"),
            new Rule(Category.FURNITURE_DECO, "조명", "스탠드", "램프", "lamp", "거실등", "형광등", "전구",
                    "커튼", "러그", "카페트", "액자", "디퓨저", "캔들", "무드등", "꽃병"),
            new Rule(Category.FURNITURE_MAIN, "가구", "침대", "소파", "책상", "의자", "체어", "스툴", "테이블", "수납장",
                    "서랍장", "옷장"),
            new Rule(Category.FURNITURE_DIY, "벽지", "바닥재", "페인트", "시트지"),

            // 도서/문구. 올리브영 "취미/팬시"는 이어폰·폰케이스까지 담는 넓은 칸이라 BROAD 로 뺐다
            new Rule(Category.BOOKS_BOOK, "도서", "소설", "문제집", "참고서", "에세이"),
            new Rule(Category.BOOKS_STATIONERY, "볼펜", "사인펜", "젤펜", "필기구", "연필", "필통", "노트",
                    "다이어리", "문구", "사무용품", "형광펜"),
            new Rule(Category.BOOKS_HOBBY, "악기", "미술용품", "프라모델", "퍼즐"),
            new Rule(Category.BOOKS_TICKET, "굿즈", "티켓"),
            new Rule(Category.TRAVEL_GOODS, "여행용", "목베개", "어댑터"),

            // 뷰티. 바디는 "바디 스크럽"이 클렌징으로 가지 않게 먼저 본다
            new Rule(Category.BEAUTY_BODY, "바디", "핸드크림", "풋크림", "풋케어", "데오드란트"),
            new Rule(Category.BEAUTY_HAIR, "샴푸", "린스", "트리트먼트", "헤어", "염색"),
            new Rule(Category.BEAUTY_CLEANSING, "클렌징", "클렌저", "리무버", "필링", "스크럽", "멜팅밤", "크림투오일"),
            new Rule(Category.BEAUTY_SKINCARE, "스킨케어", "선케어", "선크림", "선블록", "선스틱", "선쿠션",
                    "선로션", "선프로텍터", "spf", "자외선", "토너", "스킨", "로션", "에센스", "세럼", "앰플",
                    "수분크림", "영양크림", "아이크림", "진정크림", "마스크팩", "시트마스크", "미스트"),
            // "립"만으로는 클립·슬립·그립에 걸린다
            new Rule(Category.BEAUTY_MAKEUP, "메이크업", "립스틱", "립글로스", "립밤", "립펜슬", "틴트", "틴티드",
                    "파운데이션", "쿠션", "아이섀도", "마스카라", "아이라이너", "블러셔", "컨실러", "프라이머",
                    "네일", "매니큐어", "뷰티소품"),

            // "퍼퓸"은 세탁세제·샴푸·바디로션 이름에도 붙어서 뷰티·생활용품 뒤에 본다
            new Rule(Category.LUXURY_BEAUTY, "퍼퓸", "뷰티기기"),

            // 식품은 맨 뒤 — 맛·재료 낱말이 다른 물건 이름에 자주 섞인다
            new Rule(Category.FOOD_HEALTH, "영양제", "비타민", "유산균", "프로바이오틱스", "홍삼", "오메가",
                    "단백질", "프로틴", "건강식품", "콜라겐", "루테인", "아르기닌", "비오틴", "멜라토닌"),
            new Rule(Category.FOOD_BEVERAGE, "커피", "아메리카노", "모카골드", "음료", "생수", "주스", "탄산",
                    "녹차", "홍차", "티백", "두유"),
            new Rule(Category.FOOD_SNACK, "과자", "스낵", "크래커", "초콜릿", "사탕", "젤리", "쿠키", "케이크",
                    "아이스크림", "디저트", "견과"),
            new Rule(Category.FOOD_FRESH, "과일", "사과", "바나나", "딸기", "채소", "정육", "소고기",
                    "돼지고기", "수산", "생선", "계란", "달걀", "유제품", "우유", "신선"),
            new Rule(Category.FOOD_PROCESSED, "라면", "탕면", "즉석", "통조림", "소스", "양념", "면류", "밀키트",
                    "냉동", "참치", "가공식품", "김치", "만두")
    );

    /** 운동복 단서. 의류의 남녀 구분보다 먼저 본다 */
    private static final Rule SPORTS_APPAREL = SPECIFIC.stream()
            .filter(r -> r.category() == Category.FASHION_SPORTS)
            .findFirst().orElseThrow();

    /** 넓은 분류. 대분류만 맞추는 마지막 수단이다 */
    private static final List<Rule> BROAD = List.of(
            new Rule(Category.FOOD_PROCESSED, "푸드", "식품", "food"),
            new Rule(Category.BEAUTY_SKINCARE, "뷰티", "beauty", "화장품", "코스메틱"),
            new Rule(APPAREL, "패션"),
            new Rule(Category.BOOKS_HOBBY, "취미", "팬시"),
            new Rule(Category.LIVING_DAILY, "홈리빙", "리빙", "생활용품", "건강용품", "life"),
            new Rule(Category.DIGITAL_HOME_APPLIANCE, "가전", "digital")
    );

    private static final List<String> WOMEN_HINTS = normalizeAll("여성", "여자", "우먼", "women", "레이디");
    private static final List<String> MEN_HINTS = normalizeAll("남성", "남자", "맨즈", "옴므", "mens", "men's");

    /**
     * @param category Data 서버 카테고리 문자열. 없으면 null
     * @param name     상품명. 없으면 null
     * @return 판단할 근거가 없으면 null
     */
    public static Category classify(String category, String name) {
        List<String> segments = segments(category);
        String nameText = normalize(name);
        String context = String.join(" ", segments) + " " + nameText;

        for (String segment : segments) {
            Rule rule = firstMatch(SPECIFIC, segment);
            if (rule != null) return resolve(rule, context);
        }
        if (!nameText.isEmpty()) {
            Rule rule = firstMatch(SPECIFIC, nameText);
            if (rule != null) return resolve(rule, context);
        }
        for (String segment : segments) {
            Rule rule = firstMatch(BROAD, segment);
            if (rule != null) return resolve(rule, context);
        }
        return null;
    }

    // ────────────────────────────── 내부 ──────────────────────────────

    /** 구체적인 칸이 앞에 오도록 뒤집는다. 올리브영의 쉼표 목록은 순서 의미가 없다 */
    private static List<String> segments(String category) {
        if (category == null || category.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String part : category.split("[>,]")) {
            String n = normalize(part);
            if (!n.isEmpty()) out.add(0, n);
        }
        return out;
    }

    private static Rule firstMatch(List<Rule> rules, String text) {
        for (Rule rule : rules) {
            if (rule.matches(text)) return rule;
        }
        return null;
    }

    private static Category resolve(Rule rule, String context) {
        if (rule.category() != APPAREL) return rule.category();
        // 의류: 운동복 → 남녀 순으로 단서를 찾는다.
        // 단서가 없으면 기존 CategoryMapper 관례대로 여성의류. 대분류(패션의류)는 맞는다.
        if (SPORTS_APPAREL.matches(context)) return Category.FASHION_SPORTS;
        if (WOMEN_HINTS.stream().anyMatch(context::contains)) return Category.FASHION_WOMEN;
        if (MEN_HINTS.stream().anyMatch(context::contains)) return Category.FASHION_MEN;
        return Category.FASHION_WOMEN;
    }

    /** 소문자, 공백 제거. 컬리는 "썬"으로 쓴다 */
    static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replace("썬", "선");
    }

    private static List<String> normalizeAll(String... values) {
        return Arrays.stream(values).map(DataServerCategoryMapper::normalize).toList();
    }
}
