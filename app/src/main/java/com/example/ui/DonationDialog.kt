package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.LocalIsTelevision
import kotlinx.coroutines.delay

const val HYPNOSTREAM_DONATION_URL = "https://ko-fi.com/hypnostream"

/**
 * بطاقة / زر كامل مناسب لشاشة المصادر والإعدادات (SourcesScreen)
 */
@Composable
fun SupportDeveloperButton(
    isAr: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    Surface(
        color = if (isFocused) PureWhite else Color(0x26F59E0B),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) AccentCyan else Color(0x88F59E0B)
        ),
        modifier = modifier
            .focusable(interactionSource = interaction)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Coffee,
                contentDescription = null,
                tint = if (isFocused) Slate950 else Color(0xFFF59E0B),
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAr) "ادعم تطوير التطبيق" else "Support Developer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Slate950 else PureWhite
                )
                Text(
                    text = if (isAr) "مساهمة طوعية عبر Ko-fi" else "Voluntary support via Ko-fi",
                    fontSize = 11.sp,
                    color = if (isFocused) Slate950.copy(alpha = 0.8f) else Slate300
                )
            }
        }
    }
}

/**
 * أيقونة دائرية صغيرة مناسبة للشريط العلوي في الشاشة الرئيسية (DashboardScreen)
 */
@Composable
fun SupportDeveloperIconChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    Surface(
        color = if (isFocused) PureWhite else Color(0x26F59E0B),
        shape = CircleShape,
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) AccentCyan else Color(0x88F59E0B)
        ),
        modifier = modifier
            .size(38.dp)
            .focusable(interactionSource = interaction)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Coffee,
                contentDescription = "Support Developer",
                tint = if (isFocused) Slate950 else Color(0xFFF59E0B),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * نافذة الدعم والتبرع (QR Code للتلفزيون والجوال + زر فتح المتصفح)
 */
@Composable
fun DonationDialog(
    donationUrl: String = HYPNOSTREAM_DONATION_URL,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isAr = LocalAppLanguage.current == "ar"
    val isTv = LocalIsTelevision.current
    val closeFocus = remember { FocusRequester() }

    val qrApiUrl = remember(donationUrl) {
        val encoded = Uri.encode(donationUrl)
        "https://api.qrserver.com/v1/create-qr-code/?size=320x320&data=$encoded"
    }

    LaunchedEffect(Unit) {
        try {
            delay(80L)
            closeFocus.requestFocus()
        } catch (_: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isAr) "ادعم استمرار HypnoStream" else "Support HypnoStream",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAr) {
                        "تطبيق HypnoStream مجاني بالكامل وبدون إعلانات. دعمك الطوعي يساعد في تطوير ميزات جديدة وتحسين الأداء."
                    } else {
                        "HypnoStream is 100% free and ad-free. Your voluntary support helps fund new features and ongoing updates."
                    },
                    color = Slate300,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PureWhite)
                        .padding(10.dp)
                ) {
                    AsyncImage(
                        model = qrApiUrl,
                        contentDescription = "Donation QR Code",
                        modifier = Modifier.size(150.dp)
                    )
                }

                Text(
                    text = if (isAr) {
                        "امسح الرمز بكاميرا الجوال أو افتح الرابط أدناه"
                    } else {
                        "Scan with your phone camera or open link below"
                    },
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                if (!isTv) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(donationUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Slate950
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "فتح صفحة الدعم (Ko-fi)" else "Open Ko-fi Page",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(closeFocus)
            ) {
                Text(
                    text = if (isAr) "إغلاق" else "Close",
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
