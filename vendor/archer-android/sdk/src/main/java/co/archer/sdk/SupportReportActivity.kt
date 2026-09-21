package co.archer.sdk

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

data class ArcherSupportReportDraft(
  val description: String,
  val shareDiagnostics: Boolean,
  val screenshotsJpeg: List<ByteArray> = emptyList(),
  val issueType: String? = null,
)

/**
 * Material-style support report form. Presented after a hard shake when remote
 * Support is enabled.
 */
class SupportReportActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val settings = pendingSettings ?: ArcherSettings.Support()
    val ui = settings.ui
    val autoShot = pendingAutoScreenshot

    title = ui.navigationTitle

    val root = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(48, 48, 48, 48)
    }

    root.addView(TextView(this).apply {
      text = ui.headline
      textSize = 20f
    })
    root.addView(TextView(this).apply {
      text = ui.subtitle
      textSize = 14f
    })

    var issueSpinner: Spinner? = null
    if (settings.showIssueTypePicker) {
      root.addView(TextView(this).apply { text = ui.issueTypeLabel })
      val options = listOf(
        ui.issueTypeBug,
        ui.issueTypeFeature,
        ui.issueTypeFeedback,
        ui.issueTypeOther,
      )
      issueSpinner = Spinner(this).also { spinner ->
        spinner.adapter = ArrayAdapter(
          this,
          android.R.layout.simple_spinner_dropdown_item,
          options,
        )
        root.addView(spinner)
      }
    }

    root.addView(TextView(this).apply { text = ui.descriptionLabel })
    val description = EditText(this).apply {
      hint = ui.descriptionPlaceholder
      minLines = 4
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
      )
    }
    root.addView(description)

    val screenshots = mutableListOf<ByteArray>()
    autoShot?.let { screenshots.add(it) }

    root.addView(TextView(this).apply { text = ui.attachLabel })
    root.addView(Button(this).apply {
      text = ui.addScreenshotTitle
      setOnClickListener {
        captureWindowJpeg()?.let {
          if (screenshots.size < settings.maxScreenshots) {
            screenshots.add(it)
            Toast.makeText(this@SupportReportActivity, "Screenshot attached", Toast.LENGTH_SHORT).show()
          }
        }
      }
    })

    var diagnosticsBox: CheckBox? = null
    if (settings.showDiagnosticsToggle) {
      diagnosticsBox = CheckBox(this).apply {
        text = ui.diagnosticsLabel
        isChecked = settings.diagnosticsDefaultOn
      }
      root.addView(diagnosticsBox)
    }

    root.addView(Button(this).apply {
      text = ui.sendTitle
      setOnClickListener {
        val text = description.text?.toString()?.trim().orEmpty()
        if (text.length < settings.minDescriptionLength) {
          Toast.makeText(
            this@SupportReportActivity,
            "Please add a short description",
            Toast.LENGTH_SHORT,
          ).show()
          return@setOnClickListener
        }
        val clipped = text.take(settings.maxDescriptionLength)
        val draft = ArcherSupportReportDraft(
          description = clipped,
          shareDiagnostics = diagnosticsBox?.isChecked ?: settings.diagnosticsDefaultOn,
          screenshotsJpeg = screenshots.toList(),
          issueType = issueSpinner?.selectedItem?.toString(),
        )
        isEnabled = false
        val submit = pendingSubmit
        if (submit == null) {
          finish()
          return@setOnClickListener
        }
        submit(draft) { ok ->
          runOnUiThread {
            if (ok) {
              Toast.makeText(
                this@SupportReportActivity,
                ui.successTitle,
                Toast.LENGTH_SHORT,
              ).show()
              finish()
            } else {
              isEnabled = true
              Toast.makeText(
                this@SupportReportActivity,
                "Send failed — try again",
                Toast.LENGTH_SHORT,
              ).show()
            }
          }
        }
      }
    })

    root.addView(Button(this).apply {
      text = "Cancel"
      setOnClickListener { finish() }
    })

    setContentView(root)
  }

  private fun captureWindowJpeg(): ByteArray? {
    val root = window?.decorView?.rootView ?: return null
    if (root.width <= 0 || root.height <= 0) return null
    return try {
      val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      root.draw(canvas)
      val quality = ((pendingSettings?.screenshotJPEGQuality ?: 0.55) * 100).toInt().coerceIn(10, 95)
      val out = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
      out.toByteArray()
    } catch (_: Exception) {
      null
    }
  }

  companion object {
    @Volatile private var pendingSettings: ArcherSettings.Support? = null
    @Volatile private var pendingAutoScreenshot: ByteArray? = null
    @Volatile private var pendingSubmit: ((ArcherSupportReportDraft, (Boolean) -> Unit) -> Unit)? = null

    fun present(
      context: Context,
      settings: ArcherSettings.Support,
      @Suppress("UNUSED_PARAMETER") diagnostics: Map<String, String>,
      onSubmit: (ArcherSupportReportDraft, (Boolean) -> Unit) -> Unit,
    ) {
      pendingSettings = settings
      pendingSubmit = onSubmit
      pendingAutoScreenshot = null
      val intent = Intent(context, SupportReportActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }
  }
}
