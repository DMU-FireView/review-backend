-- PostgreSQL: analysis_feedbacks.review_id 의 NOT NULL 을 푼다. (#208)
--
-- 배경
--   분석 피드백이 Data 서버 리뷰도 가리킬 수 있게 됐다(상품 번호표 product_id + external_review_id).
--   그 행은 review_id 가 null 이다. 엔티티에서 nullable = false 를 지웠지만 ddl-auto=update 는
--   기존 칼럼의 NOT NULL 을 풀지 않는다. 그래서 이 ALTER 만 손으로 실행한다.
--   신고·리뷰 피드백 외부화(#171, PR #172)의 reports / review_feedbacks 와 같은 처리다.
--
--   product_id · external_review_id 칼럼과 products FK 는 nullable 이라 배포 후 ddl-auto=update 가 추가한다.
--   이 파일에서 만들지 않는다(Hibernate 가 만드는 FK 와 이름이 달라 중복 FK 가 생길 수 있다).
--
-- 배포 순서: 이 ALTER 가 먼저다.
--   새 코드는 외부 리뷰 분석 피드백을 review_id = null 로 넣는다. ALTER 전에 배포하면 그 제출만
--   NOT NULL 위반(500)으로 실패한다. 기존 Spring 리뷰 경로와 조회는 ALTER 여부와 무관하게 동작한다.
--   ALTER 를 먼저 해도 옛 코드는 영향이 없다(옛 코드는 항상 review_id 를 채운다).
--
-- 실행 순서
--   1) 이 원본(마지막 문장 ROLLBACK)을 그대로 실행해 사전 점검 출력과 "실행 후" 결과를 본다.
--      psql -X -v ON_ERROR_STOP=1 -f docs/sql/analysis-feedback-external-target.sql
--   2) 점검이 통과하고 결과가 예상과 같으면 마지막 문장만 COMMIT 으로 바꾼 사본을 처음부터 다시 실행한다.
--   3) 새 코드를 배포한다. 배포 후 ddl-auto 가 product_id · external_review_id 와 FK 를 추가했는지
--      맨 아래 "배포 후 확인" 쿼리로 본다.
--
-- ALTER ... DROP NOT NULL 은 칼럼 정의만 바꾸고 행을 다시 쓰지 않는다. 다만 ACCESS EXCLUSIVE 잠금을
-- 잠깐 잡으므로, 분석 피드백을 읽고 쓰는 긴 트랜잭션이 있으면 lock_timeout 으로 실패한다. 그때는 전체가
-- 롤백된 것이니 잠시 뒤 파일 전체를 다시 실행한다.
--
-- 되돌리기: 외부 리뷰 분석 피드백(review_id IS NULL)이 한 건이라도 생긴 뒤에는 SET NOT NULL 로 되돌릴 수 없다.
--   그 행을 먼저 보관·삭제해야 한다. 옛 코드로 롤백 배포하는 것만으로는 이 ALTER 를 되돌릴 필요가 없다.
--
-- 기본은 ROLLBACK이다. 검토 후 실제 반영할 때만 마지막 문장을 COMMIT으로 바꾼다.
BEGIN;

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';

-- 접속 대상 확인. 테이블 이름을 스키마 없이 쓰므로 search_path 로 대상이 정해진다.
SELECT current_database() AS db, current_user AS usr, current_setting('search_path') AS search_path,
       to_regclass('analysis_feedbacks') AS analysis_feedbacks;

-- 실행 전 상태: review_id 의 NOT NULL 여부와 외부 대상 칼럼 유무(배포 전이면 없다)
SELECT a.attname, format_type(a.atttypid, a.atttypmod) AS type, a.attnotnull
FROM pg_attribute a
WHERE a.attrelid = to_regclass('analysis_feedbacks')
  AND a.attname IN ('review_id', 'product_id', 'external_review_id') AND NOT a.attisdropped
ORDER BY a.attname;

SELECT COUNT(*) AS total_rows, COUNT(review_id) AS with_review_id FROM analysis_feedbacks;

-- 사전 점검. 대상 테이블·칼럼이 예상과 다르면 ALTER 전에 중단한다.
DO $$
DECLARE
    tbl REGCLASS := to_regclass('analysis_feedbacks');
    col_type TEXT;
BEGIN
    IF tbl IS NULL THEN
        RAISE EXCEPTION 'analysis_feedbacks 테이블을 찾지 못했습니다. 접속 DB 와 search_path 를 확인하세요';
    END IF;
    SELECT format_type(atttypid, atttypmod) INTO col_type
    FROM pg_attribute WHERE attrelid = tbl AND attname = 'review_id' AND NOT attisdropped;
    IF col_type IS NULL THEN
        RAISE EXCEPTION 'analysis_feedbacks.review_id 칼럼이 없습니다. 이미 수축(칼럼 제거)된 스키마인지 확인하세요';
    END IF;
    IF col_type <> 'bigint' THEN
        RAISE EXCEPTION 'analysis_feedbacks.review_id 타입이 예상(bigint)과 다릅니다: %', col_type;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = tbl AND attname = 'review_id' AND attnotnull) THEN
        RAISE NOTICE 'analysis_feedbacks.review_id 는 이미 nullable 입니다. ALTER 는 아무것도 바꾸지 않습니다';
    END IF;
END;
$$;

ALTER TABLE analysis_feedbacks ALTER COLUMN review_id DROP NOT NULL;

-- 실행 후 확인: attnotnull 이 false 여야 한다. 행 수는 실행 전과 같아야 한다.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = to_regclass('analysis_feedbacks')
               AND attname = 'review_id' AND attnotnull) THEN
        RAISE EXCEPTION 'review_id 의 NOT NULL 이 풀리지 않았습니다';
    END IF;
END;
$$;

SELECT a.attname, a.attnotnull
FROM pg_attribute a
WHERE a.attrelid = to_regclass('analysis_feedbacks') AND a.attname = 'review_id';

SELECT COUNT(*) AS total_rows, COUNT(review_id) AS with_review_id FROM analysis_feedbacks;

ROLLBACK;

-- 배포 후 확인(읽기 전용, 위 트랜잭션과 별개로 실행):
--   product_id · external_review_id 칼럼이 생겼고 product_id 가 products 를 가리키는 FK 가 정확히 하나인지 본다.
--   이 FK 는 docs/sql/remove-dummy-products.sql 의 FK 사전 점검 목록에 들어 있다.
-- SELECT attname, format_type(atttypid, atttypmod), attnotnull FROM pg_attribute
--  WHERE attrelid = 'analysis_feedbacks'::regclass AND attname IN ('review_id', 'product_id', 'external_review_id');
-- SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint
--  WHERE conrelid = 'analysis_feedbacks'::regclass AND contype = 'f';
