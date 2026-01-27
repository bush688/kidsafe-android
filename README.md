# 仪表计算器（probeapp）

一个面向仪表/电气现场的离线计算工具集，包含热电偶/热电阻换算、探头窜量/电压、差压液位计、电缆线阻/电压降等常用功能。

## 功能特性

- 热电偶（ITS-90）正反算，支持冷端补偿
- 热电阻（Pt/Cu/BA/G53）正反算，支持二/三/四线引线补偿
- 探头窜量/电压正反算
- 差压液位计零点迁移与量程核算
- 电缆线阻与电压降计算
  - 直流（DC）模型
  - 交流（AC）模型：支持功率因数（cosφ）、频率与电感（用于感抗）参数

## 模块位置

- App 模块：`probeapp/`
- 主要入口：`probeapp/src/main/java/com/kidsafe/probe/`

## 构建与导出

项目内已提供导出任务（便于生成可安装 APK）：

- `:probeapp:exportProbeDebugApk`：导出独立版调试 APK（便于内部测试）
- `:probeapp:exportProbeReleaseApk`：导出未签名 Release APK（用于后续签名/对齐）
- `:probeapp:exportProbeReleaseAab`：导出 Release AAB（用于 Play 上架前的签名流程）

导出的 APK 默认输出到：`.build/apks/`

## 发布准备

- Google Play 素材与上架信息：见 `docs/play-store/`
- GitHub 发行说明：见 `docs/github-release/`
- 隐私政策：见 `PRIVACY_POLICY.md`
- 贡献指南：见 `CONTRIBUTING.md`

## 下载

- Google Play（发布后生效）：https://play.google.com/store/apps/details?id=com.kidsafe.probe
