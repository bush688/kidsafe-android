# GitHub 发布清单（建议）

## Release 产物

- 独立版调试 APK（用于内部测试）：运行 `:probeapp:exportProbeDebugApk`
- 未签名 Release APK（用于后续签名/对齐）：运行 `:probeapp:exportProbeReleaseApk`
- Release AAB（用于 Play 上架前的签名流程）：运行 `:probeapp:exportProbeReleaseAab`

产物默认输出到：`.build/apks/`

## 版本号建议

- 发布前手动更新 `probeapp/version.properties`
- 如需在导出时自动自增版本号，可使用 Gradle 参数 `-PautoBumpProbeVersion=true`（或环境变量 `KIDSAFE_AUTO_BUMP_PROBE_VERSION=true`）

## 仓库信息

- README：根目录 `README.md`
- LICENSE：根目录 `LICENSE`
- 版本记录：根目录 `CHANGELOG.md`

## 建议的 Release Notes 结构

- 版本号
- 关键新增/优化
- 修复列表
- 已知问题（如有）
- 下载方式（APK/Play 链接）
