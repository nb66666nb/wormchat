# -*- coding: utf-8 -*-
"""
批量登录拿 token，生成 loadtest_tokens.csv 供 JMeter WebSocket 压测使用。
用法：python gen_tokens.py <起始序号> <结束序号>
示例：
  python gen_tokens.py 1 500       # loadtest1 ~ loadtest500
  python gen_tokens.py 501 1000    # loadtest501 ~ loadtest1000
  python gen_tokens.py 1 3000      # loadtest1 ~ loadtest3000
密码统一 test123456
"""
import sys
import csv
import json
import urllib.request
import urllib.parse
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_URL = "http://localhost:6060/api/account/login"
PASSWORD = "test123456"
CONCURRENCY = 50  # 并发登录数


def login(email):
    data = urllib.parse.urlencode({
        "email": email,
        "password": PASSWORD,
        "captchaKey": "test",
        "captchaCode": "test",
    }).encode("utf-8")
    req = urllib.request.Request(BASE_URL, data=data, method="POST")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            body = json.loads(resp.read().decode("utf-8"))
            if body.get("status") == "success" and body.get("data", {}).get("token"):
                return email, body["data"]["token"]
            else:
                return email, None
    except Exception:
        return email, None


def main():
    if len(sys.argv) < 3:
        print("用法：python gen_tokens.py <起始序号> <结束序号>")
        sys.exit(1)
    start = int(sys.argv[1])
    end = int(sys.argv[2])

    emails = ["loadtest%d@qq.com" % i for i in range(start, end + 1)]
    rows = []
    fail = 0
    done = 0
    with ThreadPoolExecutor(max_workers=CONCURRENCY) as pool:
        futures = {pool.submit(login, e): e for e in emails}
        for fut in as_completed(futures):
            email, token = fut.result()
            done += 1
            if token:
                rows.append((email, token))
            else:
                fail += 1

    # 追加模式：文件不存在时写表头
    out = "loadtest_tokens.csv"
    is_new = not __import__("os").path.exists(out) or __import__("os").path.getsize(out) == 0
    with open(out, "a", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        if is_new:
            w.writerow(["email", "token"])
        w.writerows(rows)

    print(f"[{start}~{end}] 成功 {len(rows)}，失败 {fail}，已追加到 {out}")


if __name__ == "__main__":
    main()
