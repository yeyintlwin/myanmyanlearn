import argparse
import glob
import json
import os
import re
import sys
from urllib.parse import urlparse


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


def connect_mysql(host, port, user, password, database):
  try:
    import mysql.connector  # type: ignore

    conn = mysql.connector.connect(
      host=host,
      port=port,
      user=user,
      password=password,
      database=database,
      autocommit=False,
    )
    return conn
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
      autocommit=False,
      charset="utf8mb4",
    )
    return conn
  except Exception:
    pass

  raise RuntimeError(
    "No MySQL driver found. Install one: pip install mysql-connector-python OR pip install pymysql"
  )


def read_text(path):
  with open(path, "r", encoding="utf-8") as f:
    return f.read()


def read_json(path):
  with open(path, "r", encoding="utf-8") as f:
    return json.load(f)


def normalize_resource_path(resources_root, path_value):
  if not isinstance(path_value, str) or not path_value.strip():
    return None
  clean = path_value.strip().lstrip("/")
  return os.path.join(resources_root, clean)


def fetch_one_value(cur, sql, params):
  cur.execute(sql, params)
  row = cur.fetchone()
  if row is None:
    return None
  if isinstance(row, dict):
    return next(iter(row.values()))
  return row[0]


def ensure_course(cur, course):
  cur.execute(
    """
    INSERT INTO courses (
      course_id, title, description, language, cover_image_url, target_students_json, published
    )
    VALUES (%s, %s, %s, %s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
      title = VALUES(title),
      description = VALUES(description),
      language = VALUES(language),
      cover_image_url = VALUES(cover_image_url),
      target_students_json = VALUES(target_students_json),
      published = VALUES(published)
    """.strip(),
    (
      course["course_id"],
      course["title"],
      course["description"],
      course["language"],
      course["cover_image_url"],
      course["target_students_json"],
      course["published"],
    ),
  )


def ensure_chapter(cur, course_id, chapter_number, chapter_uid, name):
  cur.execute(
    """
    INSERT INTO course_chapters (course_id, chapter_uid, chapter_number, name)
    VALUES (%s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
      chapter_uid = VALUES(chapter_uid),
      name = VALUES(name)
    """.strip(),
    (course_id, chapter_uid, chapter_number, name),
  )
  return fetch_one_value(
    cur,
    "SELECT id FROM course_chapters WHERE course_id = %s AND chapter_number = %s",
    (course_id, chapter_number),
  )


def ensure_subchapter(cur, chapter_id, subchapter_number, subchapter_uid, name, markdown):
  cur.execute(
    """
    INSERT INTO course_subchapters (chapter_id, subchapter_uid, subchapter_number, name, markdown)
    VALUES (%s, %s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
      subchapter_uid = VALUES(subchapter_uid),
      name = VALUES(name),
      markdown = VALUES(markdown)
    """.strip(),
    (chapter_id, subchapter_uid, subchapter_number, name, markdown),
  )
  return fetch_one_value(
    cur,
    "SELECT id FROM course_subchapters WHERE chapter_id = %s AND subchapter_number = %s",
    (chapter_id, subchapter_number),
  )


def ensure_question(cur, chapter_id, question_number, question_uid, question_markdown, explanation_markdown):
  cur.execute(
    """
    INSERT INTO course_questions (
      chapter_id, question_uid, question_number, question_markdown, explanation_markdown
    )
    VALUES (%s, %s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
      question_uid = VALUES(question_uid),
      question_markdown = VALUES(question_markdown),
      explanation_markdown = VALUES(explanation_markdown)
    """.strip(),
    (chapter_id, question_uid, question_number, question_markdown, explanation_markdown),
  )
  return fetch_one_value(
    cur,
    "SELECT id FROM course_questions WHERE chapter_id = %s AND question_number = %s",
    (chapter_id, question_number),
  )


def ensure_question_option(cur, question_id, option_index, option_content, is_correct):
  cur.execute(
    """
    INSERT INTO course_question_options (question_id, option_index, option_content, is_correct)
    VALUES (%s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
      option_content = VALUES(option_content),
      is_correct = VALUES(is_correct)
    """.strip(),
    (question_id, option_index, option_content, 1 if is_correct else 0),
  )


def ensure_slot(cur, question_id, slot_index):
  cur.execute(
    """
    INSERT INTO course_question_slots (question_id, slot_index)
    VALUES (%s, %s)
    ON DUPLICATE KEY UPDATE
      slot_index = VALUES(slot_index)
    """.strip(),
    (question_id, slot_index),
  )
  return fetch_one_value(
    cur,
    "SELECT id FROM course_question_slots WHERE question_id = %s AND slot_index = %s",
    (question_id, slot_index),
  )


def ensure_slot_option(cur, question_slot_id, option_index, option_content, is_correct):
  cur.execute(
    """
    INSERT INTO course_question_slot_options (
      question_slot_id, option_index, option_content, is_correct
    )
    VALUES (%s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
      option_content = VALUES(option_content),
      is_correct = VALUES(is_correct)
    """.strip(),
    (question_slot_id, option_index, option_content, 1 if is_correct else 0),
  )


def safe_int(value):
  try:
    return int(value)
  except Exception:
    return None


def guess_explanation_path(course_id, resources_root, chapter_number, question_number):
  candidate = os.path.join(
    resources_root,
    "courses",
    course_id,
    "questions",
    f"chapter-{chapter_number}-e{question_number}.md",
  )
  if os.path.exists(candidate):
    return candidate
  return None


def load_course_model(course_dir, resources_root, course_id_override=None):
  course_json_path = os.path.join(course_dir, "course.json")
  course_json = read_json(course_json_path)
  course_id = course_id_override or course_json.get("id") or os.path.basename(course_dir)

  course = {
    "course_id": course_id,
    "title": course_json.get("title") or course_id,
    "description": course_json.get("description"),
    "language": course_json.get("language"),
    "cover_image_url": course_json.get("cover_image_url") or course_json.get("logo"),
    "target_students_json": course_json.get("target_students_json"),
    "published": 1,
  }

  chapters = []
  for ch in course_json.get("contents", []) or []:
    chapter_number = safe_int(ch.get("order"))
    if chapter_number is None:
      continue
    chapter_label = (ch.get("chapter") or "").strip()
    chapter_title = (ch.get("title") or "").strip()
    chapter_name = (chapter_label + " " + chapter_title).strip() or chapter_title or chapter_label
    chapter_uid = f"chapter-{chapter_number}"

    subchapters = []
    for sc in ch.get("subcontents", []) or []:
      sub_number = safe_int(sc.get("order"))
      if sub_number is None:
        continue
      md_path = sc.get("markdownPath")
      if md_path:
        md_full = normalize_resource_path(resources_root, md_path)
      else:
        md_full = None
      markdown = read_text(md_full) if md_full and os.path.exists(md_full) else ""
      subchapters.append(
        {
          "subchapter_number": sub_number,
          "subchapter_uid": f"{chapter_uid}-sub-{sub_number}",
          "name": (sc.get("title") or "").strip() or f"Subchapter {sub_number}",
          "markdown": markdown,
        }
      )

    chapters.append(
      {
        "chapter_number": chapter_number,
        "chapter_uid": chapter_uid,
        "name": chapter_name,
        "subchapters": subchapters,
      }
    )

  questions_dir = os.path.join(course_dir, "questions")
  question_json_files = sorted(glob.glob(os.path.join(questions_dir, "chapter-*-q*.json")))
  questions = []
  for qpath in question_json_files:
    q = read_json(qpath)
    chapter_number = safe_int(q.get("chapterId"))
    question_number = safe_int(q.get("questionNumber"))
    if chapter_number is None or question_number is None:
      m = re.search(r"chapter-(\d+)-q(\d+)\.json$", qpath)
      if m:
        chapter_number = chapter_number if chapter_number is not None else safe_int(m.group(1))
        question_number = question_number if question_number is not None else safe_int(m.group(2))
    if chapter_number is None or question_number is None:
      continue

    question_md_path = normalize_resource_path(resources_root, q.get("questionContentPath"))
    question_md = read_text(question_md_path) if question_md_path and os.path.exists(question_md_path) else ""

    explanation_path = guess_explanation_path(course_id, resources_root, chapter_number, question_number)
    explanation_md = read_text(explanation_path) if explanation_path else ""

    questions.append(
      {
        "chapter_number": chapter_number,
        "question_number": question_number,
        "question_uid": f"c{chapter_number}-q{question_number}",
        "question_markdown": question_md,
        "explanation_markdown": explanation_md,
        "options": q.get("options"),
        "slotOptions": q.get("slotOptions"),
      }
    )

  return course, chapters, questions


def print_plan(course, chapters, questions):
  subchapter_count = sum(len(c["subchapters"]) for c in chapters)
  slot_count = 0
  slot_option_count = 0
  question_option_count = 0

  for q in questions:
    slot_options = q.get("slotOptions")
    if isinstance(slot_options, list):
      slot_count += len(slot_options)
      for opts in slot_options:
        if isinstance(opts, list):
          slot_option_count += len(opts)
    options = q.get("options")
    if isinstance(options, list):
      question_option_count += len(options)

  sys.stdout.write("Course: " + course["course_id"] + "\n")
  sys.stdout.write("Chapters: " + str(len(chapters)) + "\n")
  sys.stdout.write("Subchapters: " + str(subchapter_count) + "\n")
  sys.stdout.write("Questions: " + str(len(questions)) + "\n")
  sys.stdout.write("Slots: " + str(slot_count) + "\n")
  sys.stdout.write("Slot options: " + str(slot_option_count) + "\n")
  sys.stdout.write("Question options: " + str(question_option_count) + "\n")


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument("--properties", default="src/main/resources/application.properties")
  parser.add_argument("--resources-root", default="src/main/resources")
  parser.add_argument("--course-id", required=True)
  parser.add_argument("--course-dir")
  parser.add_argument("--dry-run", action="store_true")
  parser.add_argument("--host")
  parser.add_argument("--port", type=int)
  parser.add_argument("--database")
  parser.add_argument("--user")
  parser.add_argument("--password")
  args = parser.parse_args()

  resources_root = args.resources_root
  if args.course_dir:
    course_dir = args.course_dir
  else:
    course_dir = os.path.join(resources_root, "courses", args.course_id)

  course, chapters, questions = load_course_model(course_dir, resources_root, course_id_override=args.course_id)

  if args.dry_run:
    print_plan(course, chapters, questions)
    return

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

  conn = connect_mysql(host, port, user, password, database)
  try:
    cur = conn.cursor()

    ensure_course(cur, course)

    chapter_id_by_number = {}
    for ch in chapters:
      chapter_id = ensure_chapter(
        cur,
        course["course_id"],
        ch["chapter_number"],
        ch["chapter_uid"],
        ch["name"],
      )
      chapter_id_by_number[ch["chapter_number"]] = chapter_id
      for sc in ch["subchapters"]:
        ensure_subchapter(
          cur,
          chapter_id,
          sc["subchapter_number"],
          sc["subchapter_uid"],
          sc["name"],
          sc["markdown"],
        )

    for q in questions:
      chapter_id = chapter_id_by_number.get(q["chapter_number"])
      if not chapter_id:
        chapter_id = ensure_chapter(
          cur,
          course["course_id"],
          q["chapter_number"],
          f"chapter-{q['chapter_number']}",
          f"Chapter {q['chapter_number']}",
        )
        chapter_id_by_number[q["chapter_number"]] = chapter_id

      question_id = ensure_question(
        cur,
        chapter_id,
        q["question_number"],
        q["question_uid"],
        q["question_markdown"],
        q["explanation_markdown"],
      )

      slot_options = q.get("slotOptions")
      if isinstance(slot_options, list):
        for slot_index, options in enumerate(slot_options):
          slot_id = ensure_slot(cur, question_id, slot_index)
          if isinstance(options, list):
            for opt in options:
              if not isinstance(opt, dict):
                continue
              ensure_slot_option(
                cur,
                slot_id,
                safe_int(opt.get("optionIndex")) or 0,
                opt.get("optionContent") or "",
                bool(opt.get("isCorrect")),
              )

      options = q.get("options")
      if isinstance(options, list):
        for opt in options:
          if not isinstance(opt, dict):
            continue
          ensure_question_option(
            cur,
            question_id,
            safe_int(opt.get("optionIndex")) or 0,
            opt.get("optionContent") or "",
            bool(opt.get("isCorrect")),
          )

    conn.commit()
    sys.stdout.write("Done.\n")
  except Exception:
    try:
      conn.rollback()
    except Exception:
      pass
    raise
  finally:
    try:
      conn.close()
    except Exception:
      pass


if __name__ == "__main__":
  main()

