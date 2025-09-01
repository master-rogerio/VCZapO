package com.pdm.vczap_o.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    background = Black,
    primary = Orange,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    background = White,
    primary = Orange,
    secondary = PurpleGrey40,
    tertiary = Pink40

)

@Composable
fun VCZapoTheme(
    themeMode: com.pdm.vczap_o.core.model.ThemeMode = com.pdm.vczap_o.core.model.ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        com.pdm.vczap_o.core.model.ThemeMode.DARK -> true
        com.pdm.vczap_o.core.model.ThemeMode.LIGHT -> false
        com.pdm.vczap_o.core.model.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}



//import android.os.Build
//import androidx.compose.foundation.isSystemInDarkTheme
//import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
//import androidx.compose.material3.MaterialExpressiveTheme
//import androidx.compose.material3.MotionScheme
//import androidx.compose.material3.darkColorScheme
//import androidx.compose.material3.dynamicDarkColorScheme
//import androidx.compose.material3.dynamicLightColorScheme
//import androidx.compose.material3.lightColorScheme
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.SideEffect
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import com.pdm.vczap_o.core.model.ThemeMode
//import com.google.accompanist.systemuicontroller.rememberSystemUiController
//
//private val DarkColorScheme = darkColorScheme()
//
//private val LightColorScheme = lightColorScheme()
//
//@OptIn(ExperimentalMaterial3ExpressiveApi::class)
//@Composable
//fun VCZapoTheme(
//    themeMode: ThemeMode = ThemeMode.SYSTEM,
//    dynamicColor: Boolean = false,
//    content: @Composable () -> Unit,
//) {
//    val darkTheme = when (themeMode) {
//        ThemeMode.DARK -> true
//        ThemeMode.LIGHT -> false
//        ThemeMode.SYSTEM -> isSystemInDarkTheme()
//    }
//
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
//
//    val systemUiController = rememberSystemUiController()
//
//    SideEffect {
//        systemUiController.setStatusBarColor(
//            color = Color.Transparent,
//            darkIcons = !darkTheme
//        )
//    }
//
//    MaterialExpressiveTheme(
//        colorScheme = colorScheme,
//        typography = Typography,
//        content = content,
//        motionScheme = MotionScheme.expressive()
//    )
//}
