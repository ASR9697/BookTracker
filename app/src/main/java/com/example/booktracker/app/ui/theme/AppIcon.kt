package com.example.booktracker.app.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.example.booktracker.app.R

/**
 * The launcher icons the reader can pick. Each is an `<activity-alias>` in the
 * manifest; switching enables one alias and disables the rest, so the choice
 * lives in the package manager rather than in settings (and survives restarts
 * without the app running). Some launchers take a few seconds to redraw.
 */
enum class AppIcon(
    val title: String,
    private val alias: String,
    @DrawableRes val foreground: Int,
    @DrawableRes val background: Int
) {
    DEFAULT("Classic","LauncherDefault", R.drawable.ic_launcher_foreground, R.drawable.ic_launcher_background),
    GOLDEN_TOME("Golden Tome", "LauncherGoldenTome", R.drawable.ic_launcher_tome_foreground, R.drawable.ic_launcher_tome_background),
    MINIMALIST_MOON("Minimalist Moon", "LauncherMoon", R.drawable.ic_launcher_moon_foreground, R.drawable.ic_launcher_moon_background),
    FOUNTAIN_PEN("Fountain Pen", "LauncherFountainPen", R.drawable.ic_launcher_pen_foreground, R.drawable.ic_launcher_pen_background),
    PIXEL_BOOKSHELF("Pixel Bookshelf", "LauncherPixelShelf", R.drawable.ic_launcher_shelf_foreground, R.drawable.ic_launcher_shelf_background);

    // Aliases are declared relative to the code namespace, not the application id.
    private fun component(context: Context) = ComponentName(context, "com.example.booktracker.app.$alias")

    companion object {
        fun current(context: Context): AppIcon {
            val pm = context.packageManager
            return entries.firstOrNull { icon ->
                when (pm.getComponentEnabledSetting(icon.component(context))) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                    // Untouched aliases follow the manifest, where only the default is enabled.
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == DEFAULT
                    else -> false
                }
            } ?: DEFAULT
        }

        /** Enables [icon]'s launcher entry before disabling the others, so there is never no way in. */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                icon.component(context),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(
                    it.component(context),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
