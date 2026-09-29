-- Workbench: 먼저 왼쪽 SCHEMAS에서 Dev-archive 데이터베이스를 더블클릭하세요.
-- 기존 categories / archives 테이블은 수정하거나 삭제하지 않습니다.
CREATE TABLE IF NOT EXISTS archive_state (
 id BIGINT NOT NULL PRIMARY KEY,
 revision BIGINT NOT NULL DEFAULT 0,
 initialized BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;
INSERT INTO archive_state (id, revision, initialized)
VALUES (1, 0, FALSE)
ON DUPLICATE KEY UPDATE id = id;
SELECT id, revision, initialized FROM archive_state;
