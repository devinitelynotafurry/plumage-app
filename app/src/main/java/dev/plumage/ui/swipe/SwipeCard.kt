package dev.plumage.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Hoisted so the on-screen keep/bury buttons animate the same card the finger does,
 * instead of mutating the deck out from under a card sitting at rest.
 */
class SwipeCardState {
    val offsetX = Animatable(0f)
    val offsetY = Animatable(0f)

    var cardWidth by mutableFloatStateOf(1f)
    var cardHeight by mutableFloatStateOf(1f)

    /**
     * Where the finger landed, as a fraction of card height. Grabbing near the bottom
     * of a card should tip it the other way, the same as pushing a photo across a
     * table. Without this the rotation always pivots from the top and the card reads
     * as a sprite rather than an object.
     */
    var grabAnchor by mutableFloatStateOf(0f)

    val dragProgress: Float
        get() = (offsetX.value / (cardWidth * SWIPE_THRESHOLD_FRACTION)).coerceIn(-1f, 1f)

    val rotation: Float
        get() = (offsetX.value / (cardWidth / 2f)) * MAX_ROTATION_DEGREES * grabAnchorSign

    private val grabAnchorSign: Float get() = if (grabAnchor > 0.5f) -1f else 1f

    suspend fun settle() = coroutineScope {
        val spec = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
        launch { offsetX.animateTo(0f, spec) }
        launch { offsetY.animateTo(0f, spec) }
    }

    /** Throws the card clear, reports the decision, then snaps back for the next post. */
    suspend fun commit(toRight: Boolean, onDecided: (Boolean) -> Unit) = coroutineScope {
        val target = (if (toRight) 1f else -1f) * cardWidth * 1.8f
        launch { offsetY.animateTo(offsetY.value + cardHeight * 0.12f, tween(EXIT_MS)) }
        offsetX.animateTo(target, tween(EXIT_MS, easing = FastOutLinearInEasing))
        onDecided(toRight)
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        grabAnchor = 0f
    }

    suspend fun reset() {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        grabAnchor = 0f
    }

    companion object {
        const val SWIPE_THRESHOLD_FRACTION = 0.30f
        const val MAX_ROTATION_DEGREES = 14f
        const val FLING_VELOCITY = 900f
        const val EXIT_MS = 260
    }
}

@Composable
fun SwipeCard(
    state: SwipeCardState,
    enabled: Boolean,
    scope: CoroutineScope,
    onDecided: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .onSizeChanged {
                state.cardWidth = it.width.toFloat().coerceAtLeast(1f)
                state.cardHeight = it.height.toFloat().coerceAtLeast(1f)
            }
            .graphicsLayer {
                translationX = state.offsetX.value
                translationY = state.offsetY.value
                rotationZ = state.rotation
                // A card being thrown should shrink slightly as it leaves, the way a
                // thing moving away from you does.
                val shrink = 1f - abs(state.dragProgress) * 0.04f
                scaleX = shrink
                scaleY = shrink
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val tracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = { start ->
                        tracker.resetTracking()
                        state.grabAnchor = (start.y / size.height.toFloat()).coerceIn(0f, 1f)
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        scope.launch {
                            state.offsetX.snapTo(state.offsetX.value + drag.x)
                            // Vertical movement is damped: this is a horizontal
                            // decision, and letting the card track the finger 1:1
                            // upward makes accidental diagonal swipes feel like bugs.
                            state.offsetY.snapTo(state.offsetY.value + drag.y * 0.45f)
                        }
                    },
                    onDragCancel = {
                        scope.launch { state.settle() }
                    },
                    onDragEnd = {
                        val velocityX = tracker.calculateVelocity().x
                        val past = abs(state.offsetX.value) >
                            state.cardWidth * SwipeCardState.SWIPE_THRESHOLD_FRACTION
                        val flung = abs(velocityX) > SwipeCardState.FLING_VELOCITY

                        // If the card is far enough to one side, that position takes 
                        // precedence over velocity. This stops accidental flings in the 
                        // opposite direction when releasing a card already deep in one side.
                        val dir = when {
                            past -> state.offsetX.value.sign
                            flung -> velocityX.sign
                            else -> 0f
                        }

                        scope.launch {
                            if (dir != 0f) {
                                state.commit(dir > 0, onDecided)
                            } else {
                                state.settle()
                            }
                        }
                    }
                )
            }
    ) {
        content()
        DecisionBadge(progress = state.dragProgress)
    }
}

@Composable
private fun DecisionBadge(progress: Float) {
    val keeping = progress > 0f
    val strength = abs(progress).coerceIn(0f, 1f)
    if (strength < 0.05f) return

    val label = if (keeping) "KEEP" else "BURY"
    val tint = if (keeping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = if (keeping) Alignment.Start else Alignment.End
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = strength
                    rotationZ = if (keeping) -12f else 12f
                    scaleX = 0.9f + strength * 0.1f
                    scaleY = 0.9f + strength * 0.1f
                }
                .clip(RoundedCornerShape(10.dp))
                .background(tint)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.surface
            )
        }
    }
}

/** The card resting under the top one, scaling up as the top card is dragged away. */
@Composable
fun UnderCard(progress: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reveal = abs(progress).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = 0.94f + reveal * 0.06f
                scaleY = 0.94f + reveal * 0.06f
                alpha = 0.55f + reveal * 0.45f
            }
    ) { content() }
}
