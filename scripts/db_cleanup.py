import argparse
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple


@dataclass(frozen=True)
class DbConfig:
    host: str
    port: int
    database: str
    username: str
    password: str


def parse_properties_file(path: Path) -> Dict[str, str]:
    props: Dict[str, str] = {}
    if not path.exists():
        raise FileNotFoundError(str(path))
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        k, v = line.split("=", 1)
        props[k.strip()] = v.strip()
    return props


def parse_jdbc_mysql_url(jdbc_url: str) -> Tuple[str, int, str]:
    m = re.match(r"^jdbc:mysql://([^/:?#]+)(?::(\d+))?/([^?]+)(?:\?.*)?$", jdbc_url.strip())
    if not m:
        raise ValueError(f"Unsupported JDBC url: {jdbc_url}")
    host = m.group(1)
    port = int(m.group(2)) if m.group(2) else 3306
    database = m.group(3)
    return host, port, database


def load_db_config(properties_path: Path) -> DbConfig:
    props = parse_properties_file(properties_path)

    jdbc_url = os.getenv("SPRING_DATASOURCE_URL") or props.get("spring.datasource.url")
    username = os.getenv("SPRING_DATASOURCE_USERNAME") or props.get("spring.datasource.username")
    password = os.getenv("SPRING_DATASOURCE_PASSWORD") or props.get("spring.datasource.password")

    if not jdbc_url:
        raise ValueError("Missing spring.datasource.url")
    if not username:
        raise ValueError("Missing spring.datasource.username")
    if password is None:
        raise ValueError("Missing spring.datasource.password")

    host, port, database = parse_jdbc_mysql_url(jdbc_url)
    return DbConfig(host=host, port=port, database=database, username=username, password=password)


def get_sql_plan(user_id: Optional[str]) -> List[Tuple[str, Tuple[object, ...]]]:
    if user_id:
        return [
            ("DELETE FROM assessment_scores WHERE user_id=%s", (user_id,)),
            ("DELETE FROM otp_verifications WHERE user_id=%s", (user_id,)),
            ("DELETE FROM roles WHERE user_id=%s", (user_id,)),
            ("DELETE FROM members WHERE user_id=%s", (user_id,)),
        ]
    return [
        ("DELETE FROM assessment_scores", ()),
        ("DELETE FROM otp_verifications", ()),
        ("DELETE FROM password_reset_token", ()),
        ("DELETE FROM login_attempts", ()),
        ("DELETE FROM roles", ()),
        ("DELETE FROM members", ()),
    ]


def _format_sql_preview(sql: str, params: Tuple[object, ...]) -> str:
    if not params:
        return sql
    return f"{sql}  params={params!r}"


def _connect_mysql(config: DbConfig):
    try:
        import mysql.connector  # type: ignore

        conn = mysql.connector.connect(
            host=config.host,
            port=config.port,
            database=config.database,
            user=config.username,
            password=config.password,
        )
        return conn, "mysql.connector"
    except Exception:
        pass

    try:
        import pymysql  # type: ignore

        conn = pymysql.connect(
            host=config.host,
            port=config.port,
            database=config.database,
            user=config.username,
            password=config.password,
            autocommit=False,
        )
        return conn, "pymysql"
    except Exception:
        pass

    raise RuntimeError("No MySQL driver found. Install mysql-connector-python or PyMySQL.")


def run_cleanup(config: DbConfig, sql_plan: Iterable[Tuple[str, Tuple[object, ...]]]) -> List[int]:
    conn, driver = _connect_mysql(config)
    try:
        cursor = conn.cursor()
        try:
            cursor.execute("SET FOREIGN_KEY_CHECKS=0")
        except Exception:
            pass

        affected: List[int] = []
        for sql, params in sql_plan:
            cursor.execute(sql, params)
            rowcount = getattr(cursor, "rowcount", -1)
            affected.append(int(rowcount) if rowcount is not None else -1)

        try:
            cursor.execute("SET FOREIGN_KEY_CHECKS=1")
        except Exception:
            pass

        conn.commit()
        return affected
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


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--config",
        default="src/main/resources/application.properties",
        help="Path to application.properties",
    )
    parser.add_argument("--user-id", default=None, help="Delete data for a specific user_id only")
    parser.add_argument("--dry-run", action="store_true", help="Print planned SQL without executing")
    args = parser.parse_args(argv)

    config_path = Path(args.config)
    config = load_db_config(config_path)
    plan = get_sql_plan(args.user_id)

    if args.dry_run:
        target = f"{config.host}:{config.port}/{config.database}"
        print(f"target={target}")
        for sql, params in plan:
            print(_format_sql_preview(sql, params))
        return 0

    affected = run_cleanup(config, plan)
    for (sql, params), count in zip(plan, affected):
        print(_format_sql_preview(sql, params))
        print(f"affected_rows={count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

