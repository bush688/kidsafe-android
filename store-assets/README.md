# Play Store 素材（仪表计算器）

本目录用于集中存放 Google Play 上架所需素材与源文件，便于后续迭代更新。

## 目录结构

- `icon/`
  - `play-icon-512.png`：Play Console 高分辨率应用图标（512×512，PNG）
- `feature-graphic/`
  - `feature-graphic-1024x500.png`：Play Feature Graphic（1024×500）
- `screenshots/`
  - `phone/`：手机截图（建议 1080×1920 或更高）
  - `tablet/`：平板截图（建议 1920×1200 或更高）
  - `raw/`：通过 adb 采集的原始截图（建议先放这里再筛选）

## 生成/采集

- 生成图标与宣传图（自动生成 PNG）：
  - 运行 `scripts/generate_store_assets.ps1`
- 采集应用真实截图（需要已安装到设备/模拟器）：
  - 运行 `scripts/capture_play_screenshots.ps1`

## 注意

- Play 商店截图建议展示真实界面与实际体验；如果使用模板/示意图，请务必在上架前替换为真实截图。

