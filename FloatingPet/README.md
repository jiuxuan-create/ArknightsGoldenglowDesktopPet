# 我的桌宠 (FloatingPet)

一个 Android 悬浮窗桌宠应用，参考"呆呆鱼桌宠"的核心思路，简化为图片型桌宠。

## 功能

- **悬浮窗显示**：需要 `SYSTEM_ALERT_WINDOW` 权限，在手机桌面任意位置显示
- **拖动移动**：手指按住桌宠拖到任意位置，位置自动记忆
- **大小调节**：主界面滑杆 60dp ~ 360dp，实时生效
- **亮度调节**：0.2 ~ 1.5 倍亮度，实时生效
- **自定义形象**：从相册选择任意 PNG/JPG 作为桌宠图片（建议透明背景 PNG）
- **单击互动**：点一下桌宠会弹跳一下
- **开机自启**：可选，开机后自动恢复桌宠
- **前台服务**：保活，不会被系统轻易杀掉

## 如何编译

1. 用 **Android Studio Hedgehog (2023.1)** 或更新版本打开本目录
2. 等待 Gradle 同步完成
3. 连接手机（开启 USB 调试），点 ▶ Run

## 项目结构

```
FloatingPet/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # 权限声明 + Service/Receiver 注册
│       ├── java/com/floatingpet/
│       │   ├── MainActivity.kt          # 主界面：权限申请 + 设置滑杆 + 选图
│       │   ├── FloatingPetService.kt   # 核心：悬浮窗、拖动、大小、亮度、图片
│       │   ├── Settings.kt              # SharedPreferences 封装
│       │   └── BootReceiver.kt          # 开机自启
│       └── res/
│           ├── layout/activity_main.xml
│           └── drawable/default_pet.xml # 默认黄色圆脸
```

## 原 APK 分析结论

原"呆呆鱼桌宠"采用 **WebView + 透明 WebM 视频(VP9 alpha)** 方案：
- 悬浮窗里放一个透明 WebView，加载 `pet.html`
- HTML 里用两个 `<video>` 标签交叉播放 webm 动画（待机/点击/拖拽/随机动作）
- 通过 `window.Android` 桥接与 Native 通信
- 动画按文件夹分类：idle / click / drag / move / random / turn

本项目简化为**单张图片**方案，不需要视频素材，直接用相册图片做桌宠。如果后续要加动画，可以在 `FloatingPetService` 里把 `ImageView` 换成 `WebView` 加载类似的 HTML。
