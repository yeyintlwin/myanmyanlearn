import argparse
import os
import re
import sys
from urllib.parse import urlparse


DDL_SQL = """
CREATE TABLE IF NOT EXISTS courses (
  course_id            VARCHAR(100)  NOT NULL,
  title                VARCHAR(255)  NOT NULL,
  description          TEXT          NULL,
  language             VARCHAR(10)   NULL,
  cover_image_url      VARCHAR(500)  NULL,
  target_students_json JSON          NULL,
  published            TINYINT(1)    NOT NULL DEFAULT 1,
  created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_chapters (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  course_id       VARCHAR(100) NOT NULL,
  chapter_uid     VARCHAR(120) NULL,
  chapter_number  INT          NOT NULL,
  name            VARCHAR(255) NOT NULL,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_course_chapter_number (course_id, chapter_number),
  UNIQUE KEY uq_course_chapter_uid    (course_id, chapter_uid),
  KEY idx_course_chapters_course_id   (course_id),
  CONSTRAINT fk_course_chapters_course
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_subchapters (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  chapter_id         BIGINT       NOT NULL,
  subchapter_uid     VARCHAR(120) NULL,
  subchapter_number  INT          NOT NULL,
  name              VARCHAR(255)  NOT NULL,
  markdown           LONGTEXT     NOT NULL,
  created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_subchapter_number (chapter_id, subchapter_number),
  UNIQUE KEY uq_subchapter_uid    (chapter_id, subchapter_uid),
  KEY idx_subchapters_chapter_id  (chapter_id),
  CONSTRAINT fk_subchapters_chapter
    FOREIGN KEY (chapter_id) REFERENCES course_chapters(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_questions (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  chapter_id           BIGINT       NOT NULL,
  question_uid         VARCHAR(120) NULL,
  question_number      INT          NOT NULL,
  question_markdown    LONGTEXT     NOT NULL,
  explanation_markdown LONGTEXT     NOT NULL,
  created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_question_number (chapter_id, question_number),
  UNIQUE KEY uq_question_uid    (chapter_id, question_uid),
  KEY idx_questions_chapter_id  (chapter_id),
  CONSTRAINT fk_questions_chapter
    FOREIGN KEY (chapter_id) REFERENCES course_chapters(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_question_options (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  question_id     BIGINT      NOT NULL,
  option_index    INT         NOT NULL,
  option_content  TEXT        NOT NULL,
  is_correct      TINYINT(1)  NOT NULL DEFAULT 0,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_question_option_index (question_id, option_index),
  KEY idx_question_options_question_id (question_id),
  CONSTRAINT fk_question_options_question
    FOREIGN KEY (question_id) REFERENCES course_questions(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_question_slots (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  question_id  BIGINT      NOT NULL,
  slot_index   INT         NOT NULL,
  created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_question_slot_index (question_id, slot_index),
  KEY idx_question_slots_question_id (question_id),
  CONSTRAINT fk_question_slots_question
    FOREIGN KEY (question_id) REFERENCES course_questions(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_question_slot_options (
  id               BIGINT      NOT NULL AUTO_INCREMENT,
  question_slot_id  BIGINT      NOT NULL,
  option_index      INT         NOT NULL,
  option_content    TEXT        NOT NULL,
  is_correct        TINYINT(1)  NOT NULL DEFAULT 0,
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_slot_option_index (question_slot_id, option_index),
  KEY idx_slot_options_slot_id (question_slot_id),
  CONSTRAINT fk_slot_options_slot
    FOREIGN KEY (question_slot_id) REFERENCES course_question_slots(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
""".strip()


def read_properties(path):
  props = {}
  with open(path, "r", encoding="utf-8") as f:
    for raw in f:
      line = raw.strip()
      if not line or line.startswith("#"):
        continue
      if "=" not in line:
        continue
      key, value = line.split("=", 1)
      props[key.strip()] = value.strip()
  return props


def parse_jdbc_mysql_url(jdbc_url):
  if not isinstance(jdbc_url, str) or not jdbc_url.strip():
    raise ValueError("spring.datasource.url is missing")
  url = jdbc_url.strip()
  if url.startswith("jdbc:"):
    url = url[len("jdbc:"):]
  parsed = urlparse(url)
  if parsed.scheme != "mysql":
    raise ValueError("Unsupported JDBC URL scheme: " + str(parsed.scheme))
  host = parsed.hostname or "localhost"
  port = parsed.port or 3306
  database = (parsed.path or "").lstrip("/") or None
  return host, port, database


def split_statements(sql):
  parts = re.split(r";\s*(?:\n|$)", sql)
  out = []
  for p in parts:
    s = p.strip()
    if s:
      out.append(s)
  return out


def connect_mysql(host, port, user, password, database):
  try:
    import mysql.connector  # type: ignore

    conn = mysql.connector.connect(
      host=host,
      port=port,
      user=user,
      password=password,
      database=database,
      autocommit=True,
    )

    def run(statements):
      cur = conn.cursor()
      for st in statements:
        cur.execute(st)
      cur.close()

    return conn, run
  except Exception:
    pass

  try:
    import pymysql  # type: ignore

    conn = pymysql.connect(
      host=host,
      port=port,
      user=user,
      password=password,
      database=database,
      autocommit=True,
      charset="utf8mb4",
    )

    def run(statements):
      with conn.cursor() as cur:
        for st in statements:
          cur.execute(st)

    return conn, run
  except Exception:
    pass

  raise RuntimeError(
    "No MySQL driver found. Install one: pip install mysql-connector-python OR pip install pymysql"
  )


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument(
    "--properties",
    default="src/main/resources/application.properties",
  )
  parser.add_argument("--dry-run", action="store_true")
  parser.add_argument("--host")
  parser.add_argument("--port", type=int)
  parser.add_argument("--database")
  parser.add_argument("--user")
  parser.add_argument("--password")
  args = parser.parse_args()

  props = read_properties(args.properties)
  jdbc_url = props.get("spring.datasource.url", "")
  host, port, database = parse_jdbc_mysql_url(jdbc_url)

  user = props.get("spring.datasource.username", "")
  password = props.get("spring.datasource.password", "")

  if args.host:
    host = args.host
  if args.port:
    port = args.port
  if args.database:
    database = args.database
  if args.user:
    user = args.user
  if args.password is not None:
    password = args.password
  if os.getenv("DB_HOST"):
    host = os.getenv("DB_HOST")
  if os.getenv("DB_PORT"):
    port = int(os.getenv("DB_PORT") or port)
  if os.getenv("DB_NAME"):
    database = os.getenv("DB_NAME")
  if os.getenv("DB_USER"):
    user = os.getenv("DB_USER")
  if os.getenv("DB_PASSWORD"):
    password = os.getenv("DB_PASSWORD")

  if not user:
    raise RuntimeError("spring.datasource.username is missing")
  if database is None or not database:
    raise RuntimeError("Database name is missing in JDBC URL")

  statements = split_statements(DDL_SQL)
  if args.dry_run:
    sys.stdout.write("\\n\\n".join(statements) + "\\n")
    return

  conn, run = connect_mysql(host, port, user, password, database)
  try:
    run(statements)
  finally:
    try:
      conn.close()
    except Exception:
      pass

  sys.stdout.write("Done. Created/verified " + str(len(statements)) + " statements.\n")


if __name__ == "__main__":
  main()
