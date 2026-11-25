package com.ramparshad.taskmanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object Dimens {
    val xs = 4.dp; val sm = 8.dp; val md = 12.dp
    val lg = 16.dp; val xl = 20.dp; val xxl = 24.dp; val xxxl = 32.dp
    val cardShape = RoundedCornerShape(20.dp)
    val chipShape = RoundedCornerShape(50)
    val fieldShape = RoundedCornerShape(14.dp)
}