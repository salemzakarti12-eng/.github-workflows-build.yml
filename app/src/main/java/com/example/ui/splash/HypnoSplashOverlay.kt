package com.example.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate400
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SplashDeepNavy = Color(0xFF061539)
private val SplashBlack = Color(0xFF000000)
private val NeonCyan = Color(0xFF00E5FF)
private val ElectricBlue = Color(0xFF2979FF)
private val IndicatorRed = Color(0xFFFF2A54)
private val IndicatorPurple = Color(0xFF8B5CF6)
private val IndicatorCyan = Color(0xFF00E5FF)

/**
 * Fullscreen animated Splash and Connection Screen for HypnoStream.
 * Renders a 3D depth-layered orbital ring where the glowing white comet
 * travels behind the TV monitor on the top arc and sweeps over the front
 * of the TV screen on the bottom arc.
 */
@Composable
fun HypnoSplashOverlay(
    modifier: Modifier = Modifier,
    statusMessage: String? = null
) {
    val isAr = LocalAppLanguage.current == "ar"

    val infiniteTransition = rememberInfiniteTransition(label = "HypnoSplashAnim")

    // Gentle breathing scale for the center play triangle (0.95f to 1.05f)
    val playScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PlayScale"
    )

    // Continuous 360 rotation for the glowing orbital comet (0f -> 360f)
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitAngle"
    )

    // Sequential pulsing wave for the 3 top indicator pills
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PillPulse"
    )

    // Soft background breathing glow
    val glowRadiusFraction by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowRadius"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBlack),
        contentAlignment = Alignment.Center
    ) {
        // Deep radial navy-blue glow in the center
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val glowRadius = (size.minDimension * 0.7f) * glowRadiusFraction
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SplashDeepNavy.copy(alpha = 0.85f),
                        SplashDeepNavy.copy(alpha = 0.45f),
                        SplashBlack.copy(alpha = 0.95f),
                        SplashBlack
                    ),
                    center = centerOffset,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = centerOffset
            )
        }

        // Center Content: Animated Emblem Canvas + Typography + Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Emblem Canvas
            Canvas(
                modifier = Modifier
                    .size(width = 240.dp, height = 200.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f - 10f

                // TV Monitor dimensions
                val tvWidth = 140f
                val tvHeight = 95f
                val tvLeft = centerX - tvWidth / 2f
                val tvTop = centerY - tvHeight / 2f - 6f
                val cornerRadius = 14f

                // 3D Orbital Ring dimensions
                val ovalWidth = tvWidth * 1.58f
                val ovalHeight = tvHeight * 0.68f
                val ovalLeft = centerX - ovalWidth / 2f
                val ovalTop = centerY - ovalHeight / 2f
                val rx = ovalWidth / 2f
                val ry = ovalHeight / 2f

                // Normalize angle to [0, 360)
                val normalizedDeg = ((orbitAngle % 360f) + 360f) % 360f

                // =========================================================
                // LAYER 1 (BACK): Top half of orbital ring (180° -> 360°)
                // Passes BEHIND the TV monitor
                // =========================================================
                rotate(degrees = -18f, pivot = Offset(centerX, centerY)) {
                    // Back cyan track
                    drawArc(
                        color = NeonCyan.copy(alpha = 0.30f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(ovalLeft, ovalTop),
                        size = Size(ovalWidth, ovalHeight),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // White comet trail while traveling BEHIND the TV (180°..360°)
                    for (i in 0..14) {
                        val trailDeg = ((normalizedDeg - i * 3.5f) + 360f) % 360f
                        if (trailDeg > 180f && trailDeg < 360f) {
                            val rad = Math.toRadians(trailDeg.toDouble())
                            val dotX = centerX + rx * cos(rad).toFloat()
                            val dotY = centerY + ry * sin(rad).toFloat()
                            val fraction = 1f - (i / 15f)
                            drawCircle(
                                color = PureWhite.copy(alpha = 0.65f * fraction),
                                radius = (3.0.dp.toPx() * fraction),
                                center = Offset(dotX, dotY)
                            )
                        }
                    }
                }

                // =========================================================
                // LAYER 2 (MIDDLE): TV Monitor, Stand, Pills & Play Button
                // =========================================================
                val frameGradient = Brush.linearGradient(
                    colors = listOf(NeonCyan, ElectricBlue),
                    start = Offset(tvLeft, tvTop),
                    end = Offset(tvLeft + tvWidth, tvTop + tvHeight)
                )

                // Monitor screen interior (solid opaque dark fill so back arc hides behind it)
                val screenPadding = 5f
                val screenWidth = tvWidth - screenPadding * 2f
                val screenHeight = tvHeight - screenPadding * 2f
                val screenLeft = tvLeft + screenPadding
                val screenTop = tvTop + screenPadding
                drawRoundRect(
                    color = Color(0xFF030814),
                    topLeft = Offset(screenLeft, screenTop),
                    size = Size(screenWidth, screenHeight),
                    cornerRadius = CornerRadius(cornerRadius - 3f, cornerRadius - 3f)
                )

                // Outer bezel frame
                drawRoundRect(
                    brush = frameGradient,
                    topLeft = Offset(tvLeft, tvTop),
                    size = Size(tvWidth, tvHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Monitor Stand Neck & Base
                val neckWidth = 14f
                val neckHeight = 16f
                val neckLeft = centerX - neckWidth / 2f
                val neckTop = tvTop + tvHeight

                drawRect(
                    brush = frameGradient,
                    topLeft = Offset(neckLeft, neckTop),
                    size = Size(neckWidth, neckHeight)
                )

                val baseWidth = 46f
                val baseHeight = 5.5f
                val baseLeft = centerX - baseWidth / 2f
                val baseTop = neckTop + neckHeight - 1f
                drawRoundRect(
                    brush = frameGradient,
                    topLeft = Offset(baseLeft, baseTop),
                    size = Size(baseWidth, baseHeight),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Three Glowing Indicator Pills at Top of Screen (Red, Purple, Cyan)
                val alphaRed = ((sin(pulsePhase.toDouble()) + 1.0) / 2.0).toFloat().coerceIn(0.35f, 1f)
                val alphaPurple = ((sin(pulsePhase.toDouble() - 2.0 * PI / 3.0) + 1.0) / 2.0).toFloat().coerceIn(0.35f, 1f)
                val alphaCyan = ((sin(pulsePhase.toDouble() - 4.0 * PI / 3.0) + 1.0) / 2.0).toFloat().coerceIn(0.35f, 1f)

                val pillHeight = 4.5f
                val pillWidth = 22f
                val pillSpacing = 6f
                val totalPillsWidth = (pillWidth * 3f) + (pillSpacing * 2f)
                val pillsStart = centerX - totalPillsWidth / 2f
                val pillsTop = screenTop + 8f

                // Red Pill (Live TV)
                drawRoundRect(
                    color = IndicatorRed.copy(alpha = alphaRed),
                    topLeft = Offset(pillsStart, pillsTop),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
                // Purple Pill (Cinema Movies)
                drawRoundRect(
                    color = IndicatorPurple.copy(alpha = alphaPurple),
                    topLeft = Offset(pillsStart + pillWidth + pillSpacing, pillsTop),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
                // Cyan Pill (Series)
                drawRoundRect(
                    color = IndicatorCyan.copy(alpha = alphaCyan),
                    topLeft = Offset(pillsStart + (pillWidth + pillSpacing) * 2f, pillsTop),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )

                // Center White-to-Cyan Breathing Play Triangle
                val playCenterX = centerX + 2f
                val playCenterY = screenTop + (screenHeight / 2f) + 5f
                val baseTriangleSize = 16f * playScale

                val playPath = Path().apply {
                    moveTo(playCenterX - baseTriangleSize * 0.7f, playCenterY - baseTriangleSize)
                    lineTo(playCenterX + baseTriangleSize * 1.1f, playCenterY)
                    lineTo(playCenterX - baseTriangleSize * 0.7f, playCenterY + baseTriangleSize)
                    close()
                }

                drawPath(
                    path = playPath,
                    brush = Brush.linearGradient(
                        colors = listOf(PureWhite, NeonCyan),
                        start = Offset(playCenterX - baseTriangleSize, playCenterY - baseTriangleSize),
                        end = Offset(playCenterX + baseTriangleSize, playCenterY + baseTriangleSize)
                    )
                )

                // =========================================================
                // LAYER 3 (FRONT): Bottom half of orbital ring (0° -> 180°)
                // Sweeps OVER / IN FRONT of the TV screen with white comet
                // =========================================================
                rotate(degrees = -18f, pivot = Offset(centerX, centerY)) {
                    // Solid bright cyan front ring sweeping across TV
                    drawArc(
                        color = NeonCyan,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(ovalLeft, ovalTop),
                        size = Size(ovalWidth, ovalHeight),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Bright white comet & cyan aura sweeping IN FRONT of the TV (0°..180°)
                    for (i in 0..16) {
                        val trailDeg = ((normalizedDeg - i * 3.5f) + 360f) % 360f
                        if (trailDeg in 0f..180f) {
                            val rad = Math.toRadians(trailDeg.toDouble())
                            val dotX = centerX + rx * cos(rad).toFloat()
                            val dotY = centerY + ry * sin(rad).toFloat()
                            val fraction = 1f - (i / 17f)

                            // Outer cyan glow halo
                            drawCircle(
                                color = NeonCyan.copy(alpha = 0.45f * fraction),
                                radius = (6.5.dp.toPx() * fraction),
                                center = Offset(dotX, dotY)
                            )
                            // Crisp white core streak
                            drawCircle(
                                color = PureWhite.copy(alpha = fraction),
                                radius = (3.4.dp.toPx() * fraction),
                                center = Offset(dotX, dotY)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Two-tone Typography: "Hypno" (White) + "Stream" (Neon Cyan)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hypno",
                    color = PureWhite,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Stream",
                    color = NeonCyan,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Subtle animated progress pill
            LinearProgressIndicator(
                color = NeonCyan,
                trackColor = Color(0xFF0F1E36),
                modifier = Modifier
                    .width(160.dp)
                    .height(3.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Localized connection & channel preparation status
            val defaultStatus = if (isAr) {
                "جاري الاتصال وتجهيز القنوات..."
            } else {
                "Connecting & preparing your channels..."
            }

            Text(
                text = statusMessage ?: defaultStatus,
                color = Slate400,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
