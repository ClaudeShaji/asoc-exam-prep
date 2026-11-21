package com.asocexamprep.ui.game

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.asocexamprep.container
import com.asocexamprep.game.Celebration
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Shows XP, level-up and badge celebrations from the game engine, one at a time. */
@Composable
fun BoxScope.CelebrationHost() {
    val context = LocalContext.current
    val game = context.container.game
    val queue = remember { mutableStateListOf<Celebration>() }
    LaunchedEffect(Unit) {
        game.celebrations.collect { c ->
            // Fold back-to-back XP notices into one.
            val last = queue.lastOrNull()
            if (c is Celebration.XpGained && last is Celebration.XpGained && queue.size > 1) {
                queue[queue.lastIndex] = Celebration.XpGained(last.amount + c.amount, last.label)
            } else queue += c
        }
    }
    val current = queue.firstOrNull()
    LaunchedEffect(current) {
        if (current != null) {
            delay(if (current is Celebration.XpGained) 1300 else 2800)
            if (queue.isNotEmpty()) queue.removeAt(0)
        }
    }
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    if (!reduceMotion && (current is Celebration.LevelUp || current is Celebration.BadgeEarned)) {
        Confetti(key = current)
    }
    AnimatedVisibility(
        visible = current != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        // Above the tab bar, clear of the top bar's back button.
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 92.dp),
    ) {
        current?.let { c -> CelebrationCard(c) }
    }
}

@Composable
private fun CelebrationCard(c: Celebration) {
    val big = c !is Celebration.XpGained
    // A plain Box (not a Card/Surface) so touches pass through to the screen underneath.
    Box(
        Modifier.fillMaxWidth().testTag("celebration")
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(
                if (big) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(16.dp),
            ),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = if (big) 16.dp else 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (c) {
                is Celebration.XpGained -> {
                    Text("+${c.amount} XP", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(c.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                is Celebration.LevelUp -> {
                    LevelMedal(c.level, Modifier.size(48.dp))
                    Column {
                        Text("Level up!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Level ${c.level} · ${c.rank}", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                is Celebration.BadgeEarned -> {
                    Icon(badgeIcon(c.badge.icon), null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Column {
                        Text("Badge: ${c.badge.title}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(c.badge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
    }
}

private data class Particle(val angle: Float, val speed: Float, val spin: Float, val color: Color, val w: Float, val h: Float)

@Composable
private fun Confetti(key: Any) {
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary, Color(0xFFF2B544), Color(0xFF8ED3D0))
    val particles = remember(key) {
        val r = Random(key.hashCode())
        List(70) { Particle((-150 + r.nextFloat() * 120).toFloat(), 700f + r.nextFloat() * 900f, r.nextFloat() * 720f, colors[r.nextInt(colors.size)], 8f + r.nextFloat() * 8f, 14f + r.nextFloat() * 10f) }
    }
    val t = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { t.animateTo(1f, tween(1800, easing = LinearEasing)) }
    if (t.value >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val sec = t.value * 1.8f
        val origin = Offset(size.width / 2, size.height * 0.25f)
        particles.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble())
            val x = origin.x + (cos(rad) * p.speed * sec).toFloat() * (if (p.angle < -90) 1f else 1f)
            val y = origin.y + (sin(rad) * p.speed * sec).toFloat() + 900f * sec * sec
            rotate(p.spin * sec, Offset(x, y)) {
                drawRect(p.color.copy(alpha = 1f - t.value * 0.6f), Offset(x, y), Size(p.w, p.h))
            }
        }
    }
}
