# 演示素材说明

本目录存放 README 中引用的演示素材，命名需与 README 引用一致。

## 目录结构

```
docs/
├── demo/           # 演示视频（mp4）
│   ├── chat.mp4        # IM 单聊/群聊演示
│   ├── robot.mp4       # AI 机器人对话演示
│   ├── meeting.mp4     # 多人视频会议演示
│   └── admin.mp4       # 管理后台演示
└── images/         # 界面截图（png）
    ├── login.png       # 登录页
    ├── chat.png        # 消息页
    ├── meeting.png     # 会议页
    └── robot.png       # 机器人页
```

## 如何录屏（Windows）

- **系统自带**：`Win + G` 打开 Xbox Game Bar，点击录制（Win10/11 自带无需安装）
- **或用 OBS**：https://obsproject.com/（免费，可录窗口）

建议：每段 30~60 秒，只录关键操作，导出 mp4。

## 如何添加到 README

视频不建议直接提交到 git（仓库会变大），推荐用 GitHub 附件方式：

1. 打开仓库任意一个 issue（没有就新建一个名为 `demo` 的 issue）
2. 把 mp4 文件**拖拽**到评论输入框
3. GitHub 自动上传并生成链接，形如：
   `https://github.com/user-attachments/assets/xxxxxxxx-xxxx-xxxx.mp4`
4. 复制该链接，替换 README 中的 `docs/demo/xxx.mp4` 为：
   `[演示视频](https://github.com/user-attachments/assets/xxxxx.mp4)`
   或直接粘贴链接（GitHub 会自动渲染成可播放的视频卡片）

截图则直接放入 `docs/images/` 提交即可（png 控制在 500KB 内最佳）。
