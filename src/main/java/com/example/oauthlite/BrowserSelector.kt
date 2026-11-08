package com.example.oauthlite

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsService

internal data class BrowserInfo(
    val packageName: String,
    val supportsCustomTabs: Boolean,
    val isDefault: Boolean
)

internal object BrowserSelector {

    private val PROBE: Uri = Uri.parse("http://www.example.com")

    fun select(pm: PackageManager, blocked: Collection<String>): BrowserInfo? {
        val browsers = browsers(pm, blocked.toSet())
        return browsers.firstOrNull { it.isDefault && it.supportsCustomTabs }
            ?: browsers.firstOrNull { it.supportsCustomTabs }
            ?: browsers.firstOrNull { it.isDefault }
            ?: browsers.firstOrNull()
    }

    private fun browsers(pm: PackageManager, blocked: Set<String>): List<BrowserInfo> {
        val probe = Intent(Intent.ACTION_VIEW, PROBE).addCategory(Intent.CATEGORY_BROWSABLE)
        val default = defaultBrowser(pm, probe)

        return queryActivities(pm, probe)
            .asSequence()
            .filter { isFullBrowser(it) }
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .filterNot { it in blocked }
            .map { pkg ->
                BrowserInfo(
                    packageName = pkg,
                    supportsCustomTabs = supportsCustomTabs(pm, pkg),
                    isDefault = pkg == default
                )
            }
            .toList()
    }

    private fun isFullBrowser(info: ResolveInfo): Boolean {
        val filter = info.filter ?: return false
        return filter.hasAction(Intent.ACTION_VIEW) &&
            filter.hasCategory(Intent.CATEGORY_BROWSABLE) &&
            filter.hasDataScheme("http") &&
            filter.hasDataScheme("https") &&
            filter.countDataAuthorities() == 0 &&
            filter.countDataPaths() == 0
    }

    private fun supportsCustomTabs(pm: PackageManager, pkg: String): Boolean {
        val intent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION).setPackage(pkg)
        return queryServices(pm, intent).isNotEmpty()
    }

    private fun defaultBrowser(pm: PackageManager, probe: Intent): String? {
        val info = resolveDefault(pm, probe) ?: return null
        val activity = info.activityInfo ?: return null
        if (activity.packageName == "android") return null
        if (activity.name.endsWith("ResolverActivity")) return null
        return activity.packageName
    }

    private fun matchAllFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PackageManager.MATCH_ALL else 0

    @Suppress("DEPRECATION")
    private fun queryActivities(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        val flags = PackageManager.GET_RESOLVED_FILTER or matchAllFlag()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            pm.queryIntentActivities(intent, flags)
        }
    }

    @Suppress("DEPRECATION")
    private fun queryServices(pm: PackageManager, intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            pm.queryIntentServices(intent, 0)
        }

    @Suppress("DEPRECATION")
    private fun resolveDefault(pm: PackageManager, intent: Intent): ResolveInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
}
