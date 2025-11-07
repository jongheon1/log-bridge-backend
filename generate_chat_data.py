#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import uuid
import random
import sys
from datetime import datetime, timedelta

# 주차별 시작 날짜 (2025-11-08 기준 지난 8주)
weeks = [
    "2025-09-13",  # 8주 전
    "2025-09-20",  # 7주 전
    "2025-09-27",  # 6주 전
    "2025-10-04",  # 5주 전
    "2025-10-11",  # 4주 전
    "2025-10-18",  # 3주 전
    "2025-10-25",  # 2주 전
    "2025-11-01"   # 1주 전 (11-01 ~ 11-07)
]

# 태그별 주차별 데이터 (최신 이미지 기준)
data = {
    "교환/사이즈": [18, 19, 20, 18, 17, 19, 20, 21],
    "교환/컬러": [35, 33, 34, 30, 28, 30, 29, 31],
    "배송/운송장조회": [23, 25, 26, 24, 23, 22, 24, 23],
    "매장문의/신촌점/운영시간": [12, 22, 11, 11, 13, 9, 18, 55],
    "매장문의/안암점/운영시간": [10, 11, 13, 12, 15, 10, 20, 50],
    "매장문의/신촌점/프로모션": [17, 30, 19, 20, 19, 20, 32, 61],
    "매장문의/안암점/프로모션": [13, 15, 18, 17, 16, 18, 30, 62],
    "매장문의/신촌점/재고문의": [12, 31, 11, 9, 12, 9, 17, 43],
    "매장문의/안암점/재고문의": [10, 9, 7, 10, 9, 8, 13, 48],
    "매장문의/교환환불안내": [7, 8, 10, 11, 13, 17, 20, 50],
    "결제/환불/카카오페이": [21, 20, 18, 17, 19, 22, 23, 25],
    "결제/환불/무통장입금": [10, 12, 11, 9, 10, 11, 10, 10],
    "결제/환불/신용카드": [11, 10, 8, 9, 10, 10, 50, 14]
}

def generate_random_datetime(base_date_str, days=7):
    """주어진 시작일부터 days일 사이의 랜덤한 datetime 생성"""
    base_date = datetime.strptime(base_date_str, "%Y-%m-%d")
    random_days = random.randint(0, days - 1)
    random_hours = random.randint(0, 23)
    random_minutes = random.randint(0, 59)
    random_seconds = random.randint(0, 59)
    return base_date + timedelta(days=random_days, hours=random_hours, minutes=random_minutes, seconds=random_seconds)

def escape_sql_string(s):
    """SQL 문자열 이스케이프"""
    return s.replace("'", "''")

# 모드 선택 (기본값: SQL 파일 생성)
mode = sys.argv[1] if len(sys.argv) > 1 else "sql"

if mode == "direct":
    # Python에서 직접 DB에 연결해서 INSERT
    try:
        import mysql.connector
    except ImportError:
        print("❌ mysql-connector-python 패키지가 필요합니다.")
        print("   설치: pip install mysql-connector-python")
        sys.exit(1)

    print("🔌 MySQL에 연결 중...")
    conn = mysql.connector.connect(
        host="localhost",
        port=3306,
        user="vocuser",
        password="vocpass1234",
        database="vocinsight"
    )
    cursor = conn.cursor()

    # 기존 데이터 삭제 (선택)
    print("🗑️  기존 데이터 삭제 중...")
    cursor.execute("DELETE FROM chats")
    conn.commit()

    print("📝 데이터 INSERT 중...")
    batch_size = 500
    values_list = []
    total_inserts = 0

    for tag_name, counts in data.items():
        for week_idx, count in enumerate(counts):
            week_start = weeks[week_idx]

            for i in range(count):
                chat_id = str(uuid.uuid4())
                created_at = generate_random_datetime(week_start, days=7)
                created_at_str = created_at.strftime("%Y-%m-%d %H:%M:%S")
                tag_names_json = f'["{escape_sql_string(tag_name)}"]'

                values_list.append((chat_id, tag_names_json, created_at_str))
                total_inserts += 1

                # 배치 단위로 INSERT
                if len(values_list) >= batch_size:
                    cursor.executemany(
                        "INSERT INTO chats (id, tag_names, chat_created_at, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())",
                        values_list
                    )
                    conn.commit()
                    print(f"  ✓ {len(values_list)}개 INSERT 완료 (총 {total_inserts}개)")
                    values_list = []

    # 남은 데이터 INSERT
    if values_list:
        cursor.executemany(
            "INSERT INTO chats (id, tag_names, chat_created_at, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())",
            values_list
        )
        conn.commit()
        print(f"  ✓ {len(values_list)}개 INSERT 완료 (총 {total_inserts}개)")

    cursor.close()
    conn.close()

    print(f"\n✅ 완료! 총 {total_inserts}개의 채팅 데이터가 INSERT 되었습니다.")

else:
    # SQL 파일 생성 (Bulk Insert 형태)
    output_file = "insert_clean_chat_data.sql"

    print("📝 SQL 파일 생성 중...")
    with open(output_file, "w", encoding="utf-8") as f:
        f.write("-- Clean Chat Data Insert Script\n")
        f.write("-- Generated from clean_data table\n")
        f.write("-- Bulk Insert 형태로 생성됨\n\n")

        # 기존 데이터 삭제 (선택사항)
        f.write("-- 기존 채팅 데이터 삭제 (필요시 주석 해제)\n")
        f.write("-- DELETE FROM chats;\n\n")

        f.write("INSERT INTO chats (id, tag_names, chat_created_at, created_at, updated_at)\nVALUES\n")

        total_inserts = 0
        values_lines = []

        for tag_name, counts in data.items():
            for week_idx, count in enumerate(counts):
                week_start = weeks[week_idx]

                for i in range(count):
                    chat_id = str(uuid.uuid4())
                    created_at = generate_random_datetime(week_start, days=7)
                    created_at_str = created_at.strftime("%Y-%m-%d %H:%M:%S")

                    # JSON 형식의 tag_names (MySQL JSON 타입)
                    tag_names_json = f'["{escape_sql_string(tag_name)}"]'

                    values_lines.append(f"  ('{chat_id}', '{tag_names_json}', '{created_at_str}', NOW(), NOW())")
                    total_inserts += 1

        # 마지막 줄은 세미콜론, 나머지는 쉼표
        f.write(",\n".join(values_lines))
        f.write(";\n")

        f.write(f"\n-- Total {total_inserts} chat records generated\n")

    print(f"✅ SQL 파일 생성 완료: {output_file}")
    print(f"📊 총 {total_inserts}개의 레코드 (단일 INSERT 문)")
    print(f"\n실행 방법:")
    print(f"  MySQL CLI: mysql -u vocuser -p vocinsight < {output_file}")
    print(f"  또는 IntelliJ/DataGrip에서 파일을 열어서 전체 실행")
    print(f"\n또는 Python에서 직접 INSERT:")
    print(f"  python3 generate_chat_data.py direct")
