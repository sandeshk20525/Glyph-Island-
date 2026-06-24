package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A highly polished, custom Nothing-style Dot Matrix text renderer.
 * Each character is rendered as a 5x7 grid of dots on a Canvas.
 */
object DotMatrixFont {
    val charMap = mapOf(
        '0' to listOf(
            "11111",
            "10001",
            "10001",
            "10001",
            "11111"
        ),
        '1' to listOf(
            "00100",
            "01100",
            "00100",
            "00100",
            "01110"
        ),
        '2' to listOf(
            "11111",
            "00001",
            "11111",
            "10000",
            "11111"
        ),
        '3' to listOf(
            "11111",
            "00001",
            "01111",
            "00001",
            "11111"
        ),
        '4' to listOf(
            "10001",
            "10001",
            "11111",
            "00001",
            "00001"
        ),
        '5' to listOf(
            "11111",
            "10000",
            "11111",
            "00001",
            "11111"
        ),
        '6' to listOf(
            "11111",
            "10000",
            "11111",
            "10001",
            "11111"
        ),
        '7' to listOf(
            "11111",
            "00001",
            "00010",
            "00100",
            "00100"
        ),
        '8' to listOf(
            "11111",
            "10001",
            "11111",
            "10001",
            "11111"
        ),
        '9' to listOf(
            "11111",
            "10001",
            "11111",
            "00001",
            "11111"
        ),
        'A' to listOf(
            "01110",
            "10001",
            "11111",
            "10001",
            "10001"
        ),
        'B' to listOf(
            "11110",
            "10001",
            "11110",
            "10001",
            "11110"
        ),
        'C' to listOf(
            "01111",
            "10000",
            "10000",
            "10000",
            "01111"
        ),
        'D' to listOf(
            "11100",
            "10010",
            "10010",
            "10010",
            "11100"
        ),
        'E' to listOf(
            "11111",
            "10000",
            "11110",
            "10000",
            "11111"
        ),
        'F' to listOf(
            "11111",
            "10000",
            "11110",
            "10000",
            "10000"
        ),
        'G' to listOf(
            "01111",
            "10000",
            "10111",
            "10001",
            "01111"
        ),
        'H' to listOf(
            "10001",
            "10001",
            "11111",
            "10001",
            "10001"
        ),
        'I' to listOf(
            "11111",
            "00100",
            "00100",
            "00100",
            "11111"
        ),
        'J' to listOf(
            "00111",
            "00010",
            "00010",
            "10010",
            "01100"
        ),
        'K' to listOf(
            "10001",
            "10010",
            "11100",
            "10010",
            "10001"
        ),
        'L' to listOf(
            "10000",
            "10000",
            "10000",
            "10000",
            "11111"
        ),
        'M' to listOf(
            "10001",
            "11011",
            "10101",
            "10001",
            "10001"
        ),
        'N' to listOf(
            "10001",
            "11001",
            "10101",
            "10011",
            "10001"
        ),
        'O' to listOf(
            "01110",
            "10001",
            "10001",
            "10001",
            "01110"
        ),
        'P' to listOf(
            "11110",
            "10001",
            "11110",
            "10000",
            "10000"
        ),
        'Q' to listOf(
            "01110",
            "10001",
            "10101",
            "10011",
            "01111"
        ),
        'R' to listOf(
            "11110",
            "10001",
            "11110",
            "10010",
            "10001"
        ),
        'S' to listOf(
            "01111",
            "10000",
            "01110",
            "00001",
            "11110"
        ),
        'T' to listOf(
            "11111",
            "00100",
            "00100",
            "00100",
            "00100"
        ),
        'U' to listOf(
            "10001",
            "10001",
            "10001",
            "10001",
            "01110"
        ),
        'V' to listOf(
            "10001",
            "10001",
            "10001",
            "01010",
            "00100"
        ),
        'W' to listOf(
            "10001",
            "10001",
            "10101",
            "11011",
            "10001"
        ),
        'X' to listOf(
            "10001",
            "01010",
            "00100",
            "01010",
            "10001"
        ),
        'Y' to listOf(
            "10001",
            "10001",
            "01010",
            "00100",
            "00100"
        ),
        'Z' to listOf(
            "11111",
            "00010",
            "00100",
            "01000",
            "11111"
        ),
        ':' to listOf(
            "00000",
            "00100",
            "00000",
            "00100",
            "00000"
        ),
        '.' to listOf(
            "00000",
            "00000",
            "00000",
            "00000",
            "00100"
        ),
        '%' to listOf(
            "11000",
            "10001",
            "00100",
            "10001",
            "00011"
        ),
        '-' to listOf(
            "00000",
            "00000",
            "11111",
            "00000",
            "00000"
        ),
        '+' to listOf(
            "00000",
            "00100",
            "01110",
            "00100",
            "00000"
        ),
        '/' to listOf(
            "00001",
            "00010",
            "00100",
            "01000",
            "10000"
        ),
        ' ' to listOf(
            "00000",
            "00000",
            "00000",
            "00000",
            "00000"
        )
    )
}

@Composable
fun DotMatrixCharacter(
    char: Char,
    dotSize: Dp = 1.5.dp,
    dotSpacing: Dp = 1.dp,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color(0x0AFFFFFF),
    modifier: Modifier = Modifier
) {
    val grid = DotMatrixFont.charMap[char.uppercaseChar()] ?: DotMatrixFont.charMap[' ']!!
    val rows = grid.size
    val cols = grid[0].length

    val widthDp = (cols * dotSize.value + (cols - 1) * dotSpacing.value).dp
    val heightDp = (rows * dotSize.value + (rows - 1) * dotSpacing.value).dp

    Canvas(modifier = modifier.width(widthDp).height(heightDp)) {
        val sizePx = dotSize.toPx()
        val spacingPx = dotSpacing.toPx()
        val radius = sizePx / 2f

        for (r in 0 until rows) {
            val rowStr = grid[r]
            for (c in 0 until cols) {
                val isActive = rowStr[c] == '1'
                val cx = c * (sizePx + spacingPx) + radius
                val cy = r * (sizePx + spacingPx) + radius

                drawCircle(
                    color = if (isActive) activeColor else inactiveColor,
                    radius = radius,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
fun DotMatrixText(
    text: String,
    dotSize: Dp = 1.5.dp,
    dotSpacing: Dp = 1.dp,
    charSpacing: Dp = 3.dp,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color(0x08FFFFFF),
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        text.forEachIndexed { index, char ->
            DotMatrixCharacter(
                char = char,
                dotSize = dotSize,
                dotSpacing = dotSpacing,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                modifier = Modifier.padding(
                    end = if (index < text.length - 1) charSpacing else 0.dp
                )
            )
        }
    }
}
