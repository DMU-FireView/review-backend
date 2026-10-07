-- PostgreSQL: 등급 경계 변경(80/50 → 70/40, #201)에 맞춰 기존 reviews.trust_grade 를 확인·재분류한다.
--
-- 배경
--   TrustGrade.fromScore 경계가 AI·Data 와 같은 70/40 으로 바뀌었다. 이미 저장된 trust_grade 는
--   다시 매기지 않으므로, 옛 경계로 저장된 행 중 경계 구간(40~50, 70~80)에 있는 행만 등급이 달라진다.
--   대부분은 더미 시드(id 900000000000~900000000032 상품, #195 SQL 로 삭제 예정)이거나 레거시
--   AiAnalysisService 가 85/55/30 고정 점수로 저장한 행이라 바뀌지 않는다. 다만 레거시
--   updateReviewRtiScores 경로는 실제 AI rti 로 등급을 저장할 수 있어(코드 리뷰에서 재현),
--   그 행이 운영에 있는지 배포 전에 확인한다.
--
-- 실행 순서
--   1) 이 원본(마지막 문장 ROLLBACK)을 그대로 실행해 미리보기 건수를 본다.
--      더미 제외 대상이 0건이면 재분류할 것이 없다. 끝.
--   2) 0건이 아니면 행 목록을 검토한 뒤 마지막 문장만 COMMIT 으로 바꾼 사본을 실행한다.
--   psql -X -v ON_ERROR_STOP=1 -f docs/sql/reclassify-review-trust-grade.sql
--
-- 더미 시드 리뷰는 #195 삭제 대상이라 재분류하지 않는다(dummy_* 범위).
BEGIN;

SET LOCAL fireview.dummy_id_min = '900000000000';
SET LOCAL fireview.dummy_id_max = '900000000032';

-- 새 경계(70/40)로 계산한 등급과 저장 등급이 다른 행 (더미 제외)
CREATE TEMP TABLE reclassify_targets ON COMMIT DROP AS
SELECT r.id,
       r.product_id,
       r.rti_score,
       r.trust_grade AS old_grade,
       CASE
           WHEN r.rti_score >= 70 THEN 'SAFE'
           WHEN r.rti_score >= 40 THEN 'SUSPICIOUS'
           ELSE 'DANGER'
       END AS new_grade
FROM reviews r
WHERE r.product_id NOT BETWEEN current_setting('fireview.dummy_id_min')::bigint
                           AND current_setting('fireview.dummy_id_max')::bigint
  AND r.trust_grade <> CASE
                           WHEN r.rti_score >= 70 THEN 'SAFE'
                           WHEN r.rti_score >= 40 THEN 'SUSPICIOUS'
                           ELSE 'DANGER'
                       END;

-- 미리보기: 전체 건수, 더미 제외 대상 건수와 전이 분포, 대상 행
SELECT count(*) AS total_reviews FROM reviews;
SELECT count(*) AS targets FROM reclassify_targets;
SELECT old_grade, new_grade, count(*) FROM reclassify_targets GROUP BY 1, 2 ORDER BY 1, 2;
SELECT * FROM reclassify_targets ORDER BY id LIMIT 100;

-- 경계 구간 밖 불일치는 옛 경계로도 설명되지 않는다(예: AI level 로 저장된 행).
-- 그런 행은 점수가 아니라 AI 판정을 저장한 것이므로 건드리지 않는다.
DELETE FROM reclassify_targets
WHERE NOT (rti_score >= 40 AND rti_score < 50)
  AND NOT (rti_score >= 70 AND rti_score < 80);
SELECT count(*) AS boundary_targets FROM reclassify_targets;

UPDATE reviews r
SET trust_grade = t.new_grade
FROM reclassify_targets t
WHERE r.id = t.id;

-- 사후 확인: 경계 구간 대상이 모두 새 등급과 일치
SELECT count(*) AS remaining_mismatch
FROM reviews r
JOIN reclassify_targets t ON t.id = r.id
WHERE r.trust_grade <> t.new_grade;

ROLLBACK;
