# 应用图标方案与使用指南

本仓库提供三套图标方案（option-a/option-b/option-c），每套都包含：

- PNG：多尺寸（16/24/32/48/64/96/128/256/512/1024），透明背景
- ICO：Windows 用（内含多尺寸）
- ICNS：macOS 用（内含 16~1024 多尺寸）
- SVG：源文件（可继续在矢量工具中编辑）

目录位置：`store-assets/app-icons/`

## 三套方案说明

- option-a：仪表盘 + 指针（偏“仪表/测量”，辨识度高，推荐作为默认）
- option-b：线性刻度 + 弧线（偏“数据/校准”，更克制）
- option-c：圆环 + 闪电（偏“电气/能量”，更醒目）

## 生成/更新图标文件集

在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate_app_icons.ps1 -Force
```

输出会覆盖到：`store-assets/app-icons/`

## Android 应用图标（当前默认）

当前应用图标已更新为 option-a 风格（自适应图标）：

- 背景色：`probeapp/src/main/res/values/colors.xml` 的 `ic_launcher_background`
- 前景矢量：`probeapp/src/main/res/drawable/ic_launcher_foreground.xml`

如果需要切换到 option-b / option-c：

1. 保持 `ic_launcher.xml` 不变（仍然使用 background + foreground）
2. 修改 `ic_launcher_background` 的颜色（与方案配色保持一致）
3. 替换 `ic_launcher_foreground.xml` 的图形（建议用简洁轮廓/少量色块，避免小尺寸糊）

## A/B 测试建议（可执行、低成本）

不依赖复杂埋点的情况下，也能做相对客观的选择：

1. 形成对比物料：
   - 从各方案的 `png/icon-512.png` 导出商店图标展示图
   - 从各方案的 `png/icon-48.png` 截取桌面/任务栏小图对比
2. 问卷收集（10~30 人即可得到方向）：
   - 5 秒识别测试：看 5 秒后能否复述“像什么/做什么”
   - 三选一投票：更喜欢哪一个（第一眼）
   - 二选一偏好：A vs B，A vs C，B vs C（减少多选偏差）
3. 决策规则（建议）：
   - 小尺寸可读性权重最高（任务栏/桌面/通知栏）
   - 其次是辨识度（与系统默认图标不混淆）
   - 最后是“酷炫程度”（容易随时间审美疲劳）

## 质量验证清单

- 小尺寸：16/24/32 下轮廓清晰，不依赖细小文字
- 深色/浅色桌面：对比度足够，外形边界明确
- 文件体积：PNG 尽量使用无损压缩，避免过大
- macOS：Finder/Launchpad 显示正常（icns 含 1024）
- Windows：任务栏/开始菜单显示正常（ico 含 256）

