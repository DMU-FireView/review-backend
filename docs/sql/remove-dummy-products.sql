-- PostgreSQL: 과거 DataInitializer 시드로 들어간 더미 상품과 그 리뷰, 종속 행을 정리한다. (#195)
-- 실행 전 백업 및 대상 DB/스키마와 아래 조회 결과를 확인한다.
--
-- 판별 조건: Data 서버 번호표(products.data_platform, products.data_product_id)가 없는 상품.
--   Product.hasDataServerAddress() 와 같은 기준이다(둘 중 하나라도 NULL 또는 공백이면 번호표 없음).
--   더미 id 는 네이버 상품 ID 이거나 900000000000 부터 시작하는 fallback 구간이다.
--
-- ⚠️ 실제 사용자 데이터도 함께 지워진다.
--   더미 상품에 대한 찜(wishlists), 장바구니(cart_items), 조회 이력(view_histories),
--   더미 리뷰에 대한 신고(reports), 리뷰 피드백(review_feedbacks), 분석 피드백(analysis_feedbacks)은
--   실제 사용자가 남긴 행이어도 상품·리뷰가 사라지면 FK 때문에 남길 수 없다.
--   아래 조회 결과에서 건수를 확인하고, 필요하면 실행 전에 따로 보관한다.
--   신고·피드백 처리 결과 알림(notifications)은 FK 가 없어 남는다(target_url 이 사라진 신고를 가리킬 수 있다).
--
-- 사용자 계정(users 와 사용자 종속 테이블)과 번호표가 있는 실제 상품은 삭제하지 않는다.
-- 스키마에 추가된 FK가 있으면 검토하여 반영한다. FK 제약은 비활성화하지 않는다.
-- 기본은 ROLLBACK이다. 검토 후 실제 반영할 때만 마지막 문장을 COMMIT으로 바꾼다.
BEGIN;

-- 예상 건수. 2026-10 운영 DB 기준 더미 상품 33건, 그 리뷰 1117건이다(#165, #195).
-- 판별 조건에 걸린 건수가 이와 다르면 실제 상품이 섞였거나 데이터가 바뀐 것이므로 중단한다.
-- 다시 확인해서 맞다고 판단될 때만 숫자를 고친다.
SET LOCAL fireview.expected_dummy_products = '33';
SET LOCAL fireview.expected_dummy_reviews = '1117';

CREATE TEMP TABLE dummy_products ON COMMIT DROP AS
SELECT id FROM products
WHERE data_platform IS NULL OR TRIM(data_platform) = ''
   OR data_product_id IS NULL OR TRIM(data_product_id) = '';

CREATE TEMP TABLE dummy_reviews ON COMMIT DROP AS
SELECT id FROM reviews WHERE product_id IN (SELECT id FROM dummy_products);

-- 대상 상품 목록과 건수
SELECT p.id, p.name, p.platform, p.naver_product_id, p.data_platform, p.data_product_id, p.created_at
FROM products p WHERE p.id IN (SELECT id FROM dummy_products) ORDER BY p.id;

-- 번호표 칼럼이 하나만 채워진 행. 대상에 들어가지만 정상 경로로는 생기지 않으므로 따로 확인한다.
SELECT id, name, data_platform, data_product_id FROM products
WHERE (data_platform IS NULL) <> (data_product_id IS NULL);

-- 전체 대비 대상 건수와 종속 테이블별 영향 건수
SELECT 'products (전체)' AS target, COUNT(*) AS cnt FROM products
UNION ALL SELECT 'products (대상)', COUNT(*) FROM dummy_products
UNION ALL SELECT 'reviews (전체)', COUNT(*) FROM reviews
UNION ALL SELECT 'reviews (대상)', COUNT(*) FROM dummy_reviews
UNION ALL SELECT 'review_reasons', COUNT(*) FROM review_reasons
    WHERE review_id IN (SELECT id FROM dummy_reviews)
UNION ALL SELECT 'analysis_feedbacks', COUNT(*) FROM analysis_feedbacks
    WHERE review_id IN (SELECT id FROM dummy_reviews)
UNION ALL SELECT 'analysis_feedback_signals', COUNT(*) FROM analysis_feedback_signals
    WHERE feedback_id IN (SELECT id FROM analysis_feedbacks WHERE review_id IN (SELECT id FROM dummy_reviews))
UNION ALL SELECT 'review_feedbacks', COUNT(*) FROM review_feedbacks
    WHERE review_id IN (SELECT id FROM dummy_reviews) OR product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'reports', COUNT(*) FROM reports
    WHERE review_id IN (SELECT id FROM dummy_reviews) OR product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'wishlists', COUNT(*) FROM wishlists WHERE product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'cart_items', COUNT(*) FROM cart_items WHERE product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'view_histories', COUNT(*) FROM view_histories WHERE product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'product_platform_links', COUNT(*) FROM product_platform_links
    WHERE product_id IN (SELECT id FROM dummy_products);

-- 더미 상품·리뷰에 사용자 데이터가 붙은 사용자별 건수(삭제 전 안내·보관용)
SELECT u.id AS user_id, u.email, t.kind, COUNT(*) AS cnt
FROM (
    SELECT user_id, 'wishlists' AS kind FROM wishlists WHERE product_id IN (SELECT id FROM dummy_products)
    UNION ALL SELECT user_id, 'cart_items' FROM cart_items WHERE product_id IN (SELECT id FROM dummy_products)
    UNION ALL SELECT reporter_id, 'reports' FROM reports
        WHERE review_id IN (SELECT id FROM dummy_reviews) OR product_id IN (SELECT id FROM dummy_products)
    UNION ALL SELECT user_id, 'review_feedbacks' FROM review_feedbacks
        WHERE review_id IN (SELECT id FROM dummy_reviews) OR product_id IN (SELECT id FROM dummy_products)
    UNION ALL SELECT user_id, 'analysis_feedbacks' FROM analysis_feedbacks
        WHERE review_id IN (SELECT id FROM dummy_reviews)
) t JOIN users u ON u.id = t.user_id
GROUP BY u.id, u.email, t.kind ORDER BY u.id, t.kind;

DO $$
DECLARE
    expected_products INTEGER := current_setting('fireview.expected_dummy_products')::INTEGER;
    expected_reviews  INTEGER := current_setting('fireview.expected_dummy_reviews')::INTEGER;
    target_products   INTEGER;
    target_reviews    INTEGER;
    products_before   INTEGER;
    users_before      INTEGER;
    affected          INTEGER;
BEGIN
    -- 대상 상품 행을 잠가 실행 중에 새 찜·장바구니 등이 붙지 못하게 한다(FK 검사가 이 잠금을 기다린다).
    PERFORM 1 FROM products WHERE id IN (SELECT id FROM dummy_products) FOR UPDATE;

    SELECT COUNT(*) INTO target_products FROM dummy_products;
    SELECT COUNT(*) INTO target_reviews FROM dummy_reviews;
    IF target_products <> expected_products OR target_reviews <> expected_reviews THEN
        RAISE EXCEPTION '대상 건수가 예상과 다릅니다: 상품 %건(예상 %), 리뷰 %건(예상 %)',
            target_products, expected_products, target_reviews, expected_reviews;
    END IF;

    SELECT COUNT(*) INTO products_before FROM products;
    SELECT COUNT(*) INTO users_before FROM users;

    -- 간접 FK: 분석 피드백 신호 -> 분석 피드백 -> 리뷰
    DELETE FROM analysis_feedback_signals WHERE feedback_id IN
        (SELECT id FROM analysis_feedbacks WHERE review_id IN (SELECT id FROM dummy_reviews));
    DELETE FROM analysis_feedbacks WHERE review_id IN (SELECT id FROM dummy_reviews);
    GET DIAGNOSTICS affected = ROW_COUNT;
    RAISE NOTICE 'analysis_feedbacks: % 건 삭제', affected;

    -- 리뷰 또는 상품을 가리키는 행. 외부 리뷰 신고·피드백은 review_id 없이 product_id 만 가진다.
    DELETE FROM review_feedbacks WHERE review_id IN (SELECT id FROM dummy_reviews)
        OR product_id IN (SELECT id FROM dummy_products);
    GET DIAGNOSTICS affected = ROW_COUNT;
    RAISE NOTICE 'review_feedbacks: % 건 삭제', affected;
    DELETE FROM reports WHERE review_id IN (SELECT id FROM dummy_reviews)
        OR product_id IN (SELECT id FROM dummy_products);
    GET DIAGNOSTICS affected = ROW_COUNT;
    RAISE NOTICE 'reports: % 건 삭제', affected;

    -- 리뷰 ElementCollection -> 리뷰
    DELETE FROM review_reasons WHERE review_id IN (SELECT id FROM dummy_reviews);
    DELETE FROM reviews WHERE id IN (SELECT id FROM dummy_reviews);
    GET DIAGNOSTICS affected = ROW_COUNT;
    RAISE NOTICE 'reviews: % 건 삭제', affected;

    -- 상품을 직접 가리키는 행
    DELETE FROM wishlists WHERE product_id IN (SELECT id FROM dummy_products);
    GET DIAGNOSTICS affected = ROW_COUNT;
    RAISE NOTICE 'wishlists: % 건 삭제', affected;
    DELETE FROM cart_items WHERE product_id IN (SELECT id FROM dummy_products);
    GET DIAGNOSTICS affected = ROW_COUNT;
    RAISE NOTICE 'cart_items: % 건 삭제', affected;
    DELETE FROM view_histories WHERE product_id IN (SELECT id FROM dummy_products);
    GET DIAGNOSTICS affected = ROW_COUNT;
    RAISE NOTICE 'view_histories: % 건 삭제', affected;

    -- 상품 ElementCollection -> 상품
    DELETE FROM product_platform_links WHERE product_id IN (SELECT id FROM dummy_products);
    DELETE FROM products WHERE id IN (SELECT id FROM dummy_products);
    GET DIAGNOSTICS affected = ROW_COUNT;
    RAISE NOTICE 'products: % 건 삭제', affected;

    -- 실제 상품과 사용자 계정이 그대로인지 확인한다.
    IF (SELECT COUNT(*) FROM products) <> products_before - target_products THEN
        RAISE EXCEPTION '대상 외 상품 건수가 바뀌었습니다';
    END IF;
    IF (SELECT COUNT(*) FROM users) <> users_before THEN
        RAISE EXCEPTION '사용자 건수가 바뀌었습니다';
    END IF;
END;
$$;

-- 실행 후 건수. 대상 상품·리뷰와 종속 행이 0건이어야 한다.
SELECT 'products (전체)' AS target, COUNT(*) AS cnt FROM products
UNION ALL SELECT 'products (번호표 없음)', COUNT(*) FROM products
    WHERE data_platform IS NULL OR TRIM(data_platform) = ''
       OR data_product_id IS NULL OR TRIM(data_product_id) = ''
UNION ALL SELECT 'reviews (전체)', COUNT(*) FROM reviews
UNION ALL SELECT 'reviews (대상 남음)', COUNT(*) FROM reviews WHERE id IN (SELECT id FROM dummy_reviews)
UNION ALL SELECT 'review_reasons (대상 남음)', COUNT(*) FROM review_reasons
    WHERE review_id IN (SELECT id FROM dummy_reviews)
UNION ALL SELECT 'analysis_feedbacks (대상 남음)', COUNT(*) FROM analysis_feedbacks
    WHERE review_id IN (SELECT id FROM dummy_reviews)
UNION ALL SELECT 'review_feedbacks (대상 남음)', COUNT(*) FROM review_feedbacks
    WHERE review_id IN (SELECT id FROM dummy_reviews) OR product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'reports (대상 남음)', COUNT(*) FROM reports
    WHERE review_id IN (SELECT id FROM dummy_reviews) OR product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'wishlists (대상 남음)', COUNT(*) FROM wishlists WHERE product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'cart_items (대상 남음)', COUNT(*) FROM cart_items WHERE product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'view_histories (대상 남음)', COUNT(*) FROM view_histories
    WHERE product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'product_platform_links (대상 남음)', COUNT(*) FROM product_platform_links
    WHERE product_id IN (SELECT id FROM dummy_products)
UNION ALL SELECT 'users (전체)', COUNT(*) FROM users;

ROLLBACK;
