package co.archer.sdk

import android.content.Context

/** Optional host override for the support report UI. If unset, Archer shows its default sheet. */
fun interface ArcherSupportReportPresenting {
  /**
   * Present UI and call [completion] with a draft, or `null` if cancelled.
   * When providing a custom UI, call [completion] exactly once.
   */
  fun presentSupportReport(
    context: Context,
    diagnostics: Map<String, String>,
    settings: ArcherSettings.Support,
    completion: (ArcherSupportReportDraft?) -> Unit,
  )
}
