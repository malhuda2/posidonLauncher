package posidon.launcher.external

import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import posidon.launcher.Home
import posidon.launcher.R
import posidon.launcher.items.App
import posidon.launcher.items.Folder
import posidon.launcher.items.Shortcut
import posidon.launcher.items.isInstalled
import posidon.launcher.storage.Settings
import posidon.launcher.tools.*
import kotlin.math.min

@RequiresApi(Build.VERSION_CODES.O)
class AddShortcutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFontSetting()
        setContentView(R.layout.add_shortcut_activity)
        val shortcut = Home.launcherApps.getPinItemRequest(intent).shortcutInfo
        val hasHostPermission = Home.launcherApps.hasShortcutHostPermission()
        if (shortcut == null || !hasHostPermission) return
        val s = Shortcut(shortcut)

        Home.launcherApps.pinShortcuts(
            shortcut.`package`,
            Shortcut.pinnedShortcuts.filter {
                it.key == s.toString()
            }.map { it.value.id },
            Process.myUserHandle()
        )

        findViewById<View>(R.id.docksearchbar).visibility = View.GONE
        findViewById<View>(R.id.battery).visibility = View.GONE

        val container = findViewById<GridLayout>(R.id.dockContainer)
        container.removeAllViews()
        val columnCount = Settings["dock:columns", 5]
        val rowCount = Settings["dock:rows", 1]
        val showLabels = Settings["dockLabelsEnabled", false]
        val notifBadgesEnabled = Settings["notif:badges", true]
        val notifBadgesShowNum = Settings["notif:badges:show_num", true]
        container.columnCount = columnCount
        container.rowCount = rowCount
        val appSize = min(when (Settings["dockicsize", 1]) {
            0 -> 64.dp.toInt()
            2 -> 84.dp.toInt()
            else -> 74.dp.toInt()
        }, ((Device.displayWidth - 32.dp) / columnCount).toInt())
        var i = 0
        while (i < columnCount * rowCount) {
            val view = LayoutInflater.from(applicationContext).inflate(R.layout.drawer_item, null)
            val img = view.findViewById<ImageView>(R.id.iconimg)
            view.findViewById<View>(R.id.iconFrame).run {
                layoutParams.height = appSize
                layoutParams.width = appSize
            }
            val item = Dock[i]
            if (showLabels && item != null) {
                view.findViewById<TextView>(R.id.icontxt).text = item.label
                view.findViewById<TextView>(R.id.icontxt).setTextColor(Settings["dockLabelColor", -0x11111112])
            }
            if (item is Folder) {
                img.setImageDrawable(item.icon)
                val badge = view.findViewById<TextView>(R.id.notificationBadge)
                if (notifBadgesEnabled) {
                    val notificationCount = item.calculateNotificationCount()
                    if (notificationCount != 0) {
                        badge.visibility = View.VISIBLE
                        badge.text = if (notifBadgesShowNum) notificationCount.toString() else ""
                        ThemeTools.generateNotificationBadgeBGnFG { bg, fg ->
                            badge.background = bg
                            badge.setTextColor(fg)
                        }
                    } else { badge.visibility = View.GONE }
                } else { badge.visibility = View.GONE }
            } else if (item is Shortcut) {
                if (item.isInstalled(packageManager)) {
                    img.setImageDrawable(item.icon)
                } else {
                    Dock[i] = null
                }
            } else if (item is App) {
                if (!item.isInstalled(packageManager)) {
                    Dock[i] = null
                    continue
                }
                val badge = view.findViewById<TextView>(R.id.notificationBadge)
                if (notifBadgesEnabled && item.notificationCount != 0) {
                    badge.visibility = View.VISIBLE
                    badge.text = if (notifBadgesShowNum) item.notificationCount.toString() else ""
                    ThemeTools.generateNotificationBadgeBGnFG(item.icon) { bg, fg ->
                        badge.background = bg
                        badge.setTextColor(fg)
                    }
                } else { badge.visibility = View.GONE }
                img.setImageDrawable(item.icon)
            }
            val finalI = i
            view.setOnClickListener {
                Dock.add(s, finalI)
                Home.setDock()
                finishAffinity()
            }
            container.addView(view)
            i++
        }
        container.layoutParams.height = appSize * rowCount + if (Settings["dockLabelsEnabled", false]) (18.dp * rowCount).toInt() else 0
    }
}