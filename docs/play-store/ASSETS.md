# Google Play 素材清单（建议）

以下为常用素材要求与建议（以 Play Console 实际提示为准）：

## 必备

- 应用图标（高分辨率）：512×512 PNG，32-bit，背景不透明
- 功能图（Feature Graphic）：1024×500 PNG/JPG
- 手机截图：至少 2 张，建议 1080×1920（或同等比例/分辨率），PNG/JPG

## 建议补齐

- 7 英寸/10 英寸平板截图（如支持）
- 宣传视频（YouTube 链接）

## 生成建议

- 截图优先覆盖：主页、每个核心功能页、计算结果页、帮助/关于页
- 统一视觉：同一主题色、同一状态栏样式、避免包含私人信息

## 仓库存放位置（本项目）

本项目已创建 `store-assets/` 目录用于归档上架素材：

- 图标：`store-assets/icon/play-icon-512.png`
- 宣传图：`store-assets/feature-graphic/feature-graphic-1024x500.png`
- 截图：`store-assets/screenshots/phone/`、`store-assets/screenshots/tablet/`
- 真实截图采集（adb）：运行 `scripts/capture_play_screenshots.ps1`，输出到 `store-assets/screenshots/raw/`
