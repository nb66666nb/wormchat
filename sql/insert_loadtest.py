# -*- coding: utf-8 -*-
"""
直接连接 MySQL，批量插入压测账号。
用法：python insert_loadtest.py <起始序号> <结束序号> [前缀]
示例：
  python insert_loadtest.py 1 200         # 插入 loadtest1 ~ loadtest200（user_id=LOAD1）
  python insert_loadtest.py 1001 3000     # 插入 loadtest1001 ~ loadtest3000
密码统一 test123456 (MD5: 47ec2dd791e31e2ef2076caf64ed9b3d)
"""
import subprocess
import sys

MYSQL = r"C:\Mysql\bin\mysql.exe"
DB = "easymetting"
USER = "root"
PWD = "2233"
MD5 = "47ec2dd791e31e2ef2076caf64ed9b3d"


def main():
    if len(sys.argv) < 3:
        print("用法：python insert_loadtest.py <起始序号> <结束序号>")
        sys.exit(1)
    start = int(sys.argv[1])
    end = int(sys.argv[2])

    values = []
    for i in range(start, end + 1):
        uid = "LOAD%d" % i
        email = "loadtest%d@qq.com" % i
        nick = "压测用户%d" % i
        meeting = "GLD%d" % i
        values.append(
            "('%s','%s','%s','%s',2,1,'%s',NOW())" % (uid, nick, MD5, email, meeting)
        )

    sql = (
        "SET NAMES utf8mb4;\n"
        "INSERT INTO `user_info` "
        "(`user_id`,`nick_name`,`password`,`email`,`sex`,`status`,`meeting_no`,`create_time`) VALUES\n"
        + ",\n".join(values)
        + "\nON DUPLICATE KEY UPDATE `email` = VALUES(`email`);\n"
    )

    proc = subprocess.run(
        [MYSQL, "-u" + USER, "-p" + PWD, DB],
        input=sql.encode("utf-8"),
        capture_output=True,
    )

    if proc.returncode == 0:
        print("插入成功：loadtest%d ~ loadtest%d" % (start, end))
    else:
        print("插入失败：")
        print(proc.stderr.decode("utf-8", errors="replace"))
        sys.exit(1)


if __name__ == "__main__":
    main()
