package dev.unknownuser

import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.dsl.ComponentBuilder
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.material3.MaterialButton
import dev.unknownuser.ananda.material3.MaterialButtonStyle
import dev.unknownuser.ananda.material3.MaterialSurface
import dev.unknownuser.ananda.material3.MaterialSurfaceLevel
import dev.unknownuser.ananda.material3.MaterialSwitch
import dev.unknownuser.ananda.material3.MaterialTextField
import dev.unknownuser.ananda.material3.MaterialTextFieldStyle
import dev.unknownuser.ananda.material3.MaterialTheme
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.window.RenderMode
import dev.unknownuser.ananda.window.SkiaWindow
import java.awt.Color

private const val DemoWidth = 960
private const val DemoHeight = 640

fun main() {
    val theme = MaterialTheme.dark(Color(94, 234, 212))
    val name = stateOf("")
    val enabled = stateOf(true)
    val root = MaterialSurface(
        width = DemoWidth.toFloat(),
        height = DemoHeight.toFloat(),
        level = MaterialSurfaceLevel.Lowest,
        shape = 0f
    )

    ComponentBuilder(root).apply {
        padding(52, 42)
        column(gap = 22, crossAxisAlignment = Alignment.Start)

        add(Label("Ananda · Material 3", size = 30f, color = theme.palette.onSurface))
        add(Label("嵌套布局、中文字体、焦点管理与 Compose 风格输入框", size = 16f, color = theme.palette.onSurfaceVariant))

        add(
            MaterialTextField(
                value = name,
                width = 430f,
                label = "玩家名称",
                placeholder = "输入中文或英文…",
                supportingText = "点击页面空白处可以失焦",
                variant = MaterialTextFieldStyle.Outlined
            )
        )

        row(gap = 12, crossAxisAlignment = Alignment.Center) {
            add(MaterialButton("保存", width = 112f) {})
            add(MaterialButton("稍后", width = 112f, variant = MaterialButtonStyle.Outlined) {})
            add(MaterialSwitch(enabled))
            add(Label("启用实时预览", size = 16f, color = theme.palette.onSurfaceVariant))
        }

        flowRow(horizontalGap = 12, verticalGap = 12) {
            add(MaterialButton("Filled", variant = MaterialButtonStyle.Filled))
            add(MaterialButton("Tonal", variant = MaterialButtonStyle.Tonal))
            add(MaterialButton("Elevated", variant = MaterialButtonStyle.Elevated))
            add(MaterialButton("Text", variant = MaterialButtonStyle.Text))
        }
    }

    val scene = Scene().apply {
        this.theme = theme
        add(root)
    }
    SkiaWindow(
        title = "Ananda Material 3 Demo",
        width = DemoWidth,
        height = DemoHeight,
        scene = scene,
        renderMode = RenderMode.Continuous
    ).show()
}
