# 羽球手账 Android 本地版 v2.0

这是一个完全本地运行的 Android App：不依赖 Chrome，不需要联网，也没有 INTERNET 权限。记录保存在 App 私有 SharedPreferences 中。

## v2.0 主要变化

- 首页重新设计为移动端极速填写界面。
- 修复 Android WebView 中 datalist 无法正常选择的问题：场馆/场地改用 Android WebView 兼容性更好的原生 `<select>`，球搭子改用多选弹层。
- 预置：
  - 邱德拔体育馆：1–12 号场地
  - 五四体育馆：1–10 号场地
- 新增“场馆库”：可以添加/编辑场馆名称、场地数量和备注。
- 新增“球搭子档案”：
  - 当前名称（可随时修改）
  - 曾用名/别名（改名时旧名称自动保留）
  - 球风
  - 喜好/习惯
  - 擅长/优势
  - 配合提示/默契
  - 其他备注
  - 按日期追加的成长轨迹
- 打球记录改用场馆 ID 和球搭子 ID 关联。以后改名，历史记录仍然属于同一场馆/同一个人。
- 旧版本数据会自动迁移，不需要重新录入。
- 新增球搭子 CSV 导出。
- 最低系统版本降至 Android 8（API 26）。Android 10+ 导出文件自动保存到 `Downloads/BadmintonLog`；Android 8/9 使用系统文件保存器。
- App 关闭 Android 云端自动备份，数据只保存在本机；请定期导出 JSON。

## GitHub Actions 构建 APK

仓库根目录必须直接看到：

```
.github/
app/
build.gradle
gradle.properties
settings.gradle
```

`.github/workflows/build-apk.yml` 已升级为当前 Actions 版本：
- actions/checkout@v7
- actions/setup-java@v6
- android-actions/setup-android@v4
- gradle/actions/setup-gradle@v6
- actions/upload-artifact@v7

提交到 main 后会自动构建，也可以到 Actions → Build Android APK → Run workflow 手动构建。
构建成功后，从该次运行页面底部下载 `badminton-log-apk`，解压得到 `app-debug.apk`。

## 更新已有仓库

如果你的 GitHub 仓库已经建立，最稳妥的方式是用本压缩包中的同名文件替换仓库内容，尤其要更新：

```
app/src/main/assets/index.html
app/src/main/java/com/example/badmintonlog/MainActivity.java
app/src/main/AndroidManifest.xml
app/src/main/res/values/styles.xml
app/build.gradle
.github/workflows/build-apk.yml
```

然后 Commit 到 `main`，Actions 会自动重新生成 APK。

## 数据备份

“备份”页可以导出：
- 完整 JSON（最重要，恢复 App 时使用）
- 打球记录 CSV
- 球搭子资料 CSV

卸载 App、清除 App 数据或换手机前，请务必先导出完整 JSON。

- v2.1：移除场馆归档；旧数据中的 qdb/QDB 会自动合并为“邱德拔体育馆”；移除首页说明句。

## v2.2 新增

- 球搭子档案增加“性别：女 / 男”字段。
- 一场球仍然只选择一个场馆，但场地改为按整点小时记录。
  例如 18:00–20:00：
  - 18:00–19:00：3号场地
  - 19:00–20:00：5号场地
- 多小时同一场地时，可使用“全部同第一小时”快速填写。
- 历史记录会显示分时场地；如果全程同一场地，则简写为“X号场地（全程）”。
- 场地统计按实际使用小时分摊。例如 3号场 1 小时、5号场 1 小时会分别累计。
- 旧版单场地记录会自动迁移为分时记录，不需要重新填写。
- 旧球搭子资料保持不变；性别为空时显示“未填写”，编辑档案时可以补充。

## v2.3：球搭子排序与筛选

- 首页“选择球搭子”默认按一起打球次数从多到少排列。
- 同场次数相同时，再按共同小时数、最近一起打日期排序。
- 选择列表直接显示共同场次、共同小时与最近一次日期。
- 球搭子档案支持：
  - 性别筛选：全部 / 女 / 男 / 未填写
  - 共同记录筛选：全部 / 一起打过 / 还没一起打
  - 排序：最常打 / 最近一起打 / 累计时长 / 姓名
- 球搭子档案卡显示最近一起打日期。

## v2.4：球搭子详情统计

每个球搭子卡片新增“详情”按钮，详情页自动显示：
- 一起打过多少场
- 累计共同打球小时
- 最近一次一起打球日期
- 常去场馆排行（共同场次 + 共同小时）
- 最近 5 条成长记录

所有统计都从已有打球记录和成长轨迹自动计算。

## v2.5：JSON 导入与覆盖更新修复

### JSON 导入
Android 端现在改为：
1. 系统文件选择器选择 JSON；
2. Android 原生代码直接读取并校验 JSON；
3. 原生写入 App 私有 SharedPreferences；
4. 页面重新加载并自动迁移旧数据。

不再依赖“把整段 JSON 注入 JavaScript”的链路。

### 固定签名与覆盖更新
此前 GitHub Actions 每次使用临时 debug 签名，因此每次 APK 的签名可能不同，Android 会要求卸载旧版。

v2.5 改为固定 release 签名。配置一次 GitHub Actions secrets 后，以后只要一直使用同一签名密钥，新的 APK 就可以直接覆盖安装旧 APK。

注意：从此前随机 debug 签名版切换到 v2.5 固定签名版时，仍需最后卸载一次旧版。之后不再需要。
