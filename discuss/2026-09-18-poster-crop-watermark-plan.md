# 封面裁剪水印功能计划

- 日期：2026-09-18
- 状态：已实施（方案 A，有码默认不选，其余按识别结果默认勾选）
- 参考实现：`mdcx/mdcx/base/image.py`（`add_mark_thread` / `_add_to_pic`）、`mdcx/mdcx/controllers/cut_window.py`、`mdcx/mdcx/config/models.py`

## 1. 背景与目标

详情页「裁剪封面」目前已完成：fanart 优先作为源图，在对话框内拖动裁剪框、调整高宽比，确认后写入 `poster.jpg`（临时文件 + 原子替换）。本次在该链路上补充 mdcx 同款水印能力：

- 裁剪对话框内选择水印类型，实时预览，保存时把水印绘制进 `poster.jpg`。
- 水印类型对齐 mdcx：4K / 8K / 字幕 / 有码 / 破解 / 流出 / 无码。
- 大小公式对齐 mdcx：水印高度 = 海报高度 × 水印大小 / 40，默认 5，可调 1-10。
- 位置对齐 mdcx 默认行为：4K/8K 固定右下角，其余水印从左上角起顺时针排布，右下被 4K/8K 占用时跳过。

## 2. mdcx 实现要点与移植范围

| mdcx 要素 | mdcx 行为 | 本次处理 |
| --- | --- | --- |
| 水印素材 | `resources/Img/{4k,8k,sub,youma,umr,leak,wuma}.png`，用户可在 `userdata/watermark` 覆盖 | 复制 7 张内置素材进应用资源；暂不支持用户自定义覆盖 |
| 类型推断 | `cut_window` 用 NFO/文件名信息（清晰度/字幕/马赛克）预选按钮，可手动改 | 用 `Video.tags` + 视频文件名推断默认勾选，可手动改 |
| 位置模式 | `not_fixed` / `fixed` / `corner` 三种，配置项较多 | 只实现默认 `not_fixed` 路径（高清右下 + 其余顺时针）；`fixed`/`corner` 留作后续 |
| 影响图片 | poster / thumb / fanart 三张都可加水印 | 只对本次裁剪产物 `poster.jpg` 加水印，不碰 thumb/fanart |
| 写入方式 | 每张图反复「打开-贴图-存临时-替换」 | 裁剪 + 水印在内存中一次完成，再按现有原子替换写盘 |

明确不做：刮削流程自动加水印、thumb/fanart 水印、自定义水印目录、`fixed`/`corner` 位置模式。接口按可扩展方向设计，后续要做时不推倒重来。

## 3. 设计

### 3.1 资源

新增 `app/src/main/resources/watermark/`，从 `mdcx/resources/Img/` 复制 7 张 PNG（合计约 1 MB），运行时从类路径 `/watermark/xxx.png` 读取。

### 3.2 领域模型（新文件 `javscraper/models/Watermark.kt`）

- `enum class WatermarkMark`：`HD_4K`、`HD_8K`、`SUB`、`CENSORED`、`UMR`、`LEAK`、`UNCENSORED`，各自绑定素材文件名。
- `data class WatermarkOptions(marks: List<WatermarkMark>, size: Int = 5)`，`size` 范围 1..10。
- 纯函数 `inferWatermarkMarks(video: Video): Set<WatermarkMark>`：
  - tags 含「中文字幕/字幕」→ `SUB`
  - tags 或视频文件名含 `4K` / `8K`（忽略大小写）→ 对应清晰度标记
  - tags 含「无码」→ `UNCENSORED`；「破解」→ `UMR`；「流出」→ `LEAK`；「有码」→ `CENSORED`
  - 推断不到就不勾选，用户可在对话框手动补选

一个默认值决策：mdcx 因马赛克信息默认值会把「有码」预选上；本计划默认不自动勾选「有码」，避免用户没查看类型就保存时被强加有码水印。若你想完全对齐 mdcx，可改为默认勾选。

### 3.3 渲染器（新文件 `javscraper/io/WatermarkRenderer.kt`）

- 输入 `BufferedImage` + `WatermarkOptions`，返回绘制后的新图，不修改入参。
- 布局与渲染分离：`layout(...)` 返回每个水印的目标矩形；UI 预览复用同一布局逻辑，保证所见即所得。
- 缩放：按「目标高度 = 图高 × size / 40」等比缩放 PNG，双线性插值，保留透明通道。
- 位置：`HD_4K`/`HD_8K` → 右下角；字幕/马赛克类按「左上 → 右上 → 右下 → 左下」顺序取角，跳过已被高清水印占用的右下角。
- 素材缺失或读取失败：跳过该水印，不阻断裁剪保存（水印是增强能力，写盘成功优先），失败信息记入日志。
- 顺带把 JPEG 输出质量真正设为 0.95 并禁用色度二次抽样，对齐 mdcx `quality=95, subsampling=0`；当前代码注释写着 quality 95，但 `ImageIO.write` 实际用的是默认低质量参数。

### 3.4 PosterCropper 集成

`cropToFile(source, dest, rect, watermark: WatermarkOptions? = null)`：

- 带默认参数，既有调用与测试不传即保持现状，零破坏。
- 裁剪后、写临时文件前调用渲染器；水印失败时回退输出无水印结果。

### 3.5 UI（PosterCropDialog）

- 新增「水印」区块：总开关、`4K/8K/无` 单选、`字幕` 复选、`有码/破解/流出/无码/无` 单选、大小滑杆。
- 预览缩略图实时叠加水印（复用渲染器布局，把素材按预览尺寸缩放绘制）。
- 保存时把用户最终选择传入 `cropToFile`。
- 开关与大小在松手后持久化，模式对齐现有 `posterCropAspect`（对话框内调整、`AppSettings` 存储）。

### 3.6 设置与 i18n

`AppSettings` 新增：

- `posterWatermarkEnabled: Boolean = true`（对齐 mdcx `poster_mark` 默认开启）
- `posterWatermarkSize: Int = 5`（1..10）

`TranslationEn` / `TranslationZh` 补齐水印相关文案（水印、水印类型、字幕、有码、破解、流出、无码、4K、8K、无、水印大小等），`TranslationsTest` 同步覆盖。

## 4. 架构问题（需要你拍板）

现有目录已超出「每层 ≤8 个文件」指标：`ui/components` 11 个、`io` 9 个。两种做法：

- 方案 A（推荐）：顺带小规模归组。新建 `ui/components/media/`，放入 `PosterCard`、`PosterCropDialog`、新水印控件、`ExtraFanartCarousel/Viewer`；`io` 侧新建 `io/image/`，放入 `PosterCropper` 与 `WatermarkRenderer`。两个根目录都回到 ≤8，改动是纯移动 + 包名/import 更新，编译器兜底。
- 方案 B（最小改动）：不动现有文件，直接新增 `Watermark.kt`（models 1→2）、`WatermarkRenderer.kt`（io 9→10）、水印控件（components 11→12），超标进一步扩大，后续再专门重构。

## 5. 测试计划（增量）

- `WatermarkRendererTest`：尺寸公式（高 × size / 40）、四角布局顺序与高清让位、透明像素确实写入、素材缺失容错。
- `PosterCropperTest`：`watermark = null` 兼容旧用例；带水印时输出尺寸不变且角落像素变化；JPEG 可读。
- `inferWatermarkMarks` 测试：tags/文件名各分支、大小写、无信息时为空。
- `SettingsManagerTest`：新字段默认值、持久化往返、越界值钳到 1..10。
- `TranslationsTest`：新文案中英文齐全。
- 验证命令：`.\gradlew :app:test --no-daemon`。Python worker 无改动，不需要重新打包 exe。

## 6. 实施步骤

1. 复制 7 张水印素材；新建 `Watermark` 模型与 `inferWatermarkMarks`，先写测试。
2. 实现 `WatermarkRenderer`（布局/渲染/容错），补测试。
3. `PosterCropper` 接入 `watermark` 参数并落实 JPEG 0.95，补测试。
4. `AppSettings` 增加两个字段；i18n 补文案；补各自测试。
5. `PosterCropDialog` 增加水印控件与实时预览（按方案 A/B 决定是否归组子包）。
6. 增量运行相关测试与 `:app:compileKotlin`，人工打开裁剪对话框验证一次保存流程。

## 7. 风险与说明

- 源图缺 fanart 时会回退使用已有 poster；若该 poster 已带水印，再勾选会二次叠加。界面不做自动检测，由用户自行判断（与 mdcx 手动裁剪行为一致）。
- 素材约 1 MB，安装包体积小幅增加。
- JPEG 重编码一次不可避免（与现状一致），质量提升到 0.95 后损失更小。