package com.example.nusukautokeys

import android.app.Activity
import android.os.Bundle
import android.os.StrictMode
import android.content.Intent
import android.net.Uri
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val api = "https://umrahcodes.click/open-data/v1/latest.json"
    private val prefs by lazy { getSharedPreferences("codes", MODE_PRIVATE) }
    private val codes = mutableListOf<String>()
    private var index = 0
    private lateinit var status: TextView
    private lateinit var codeView: TextView
    private lateinit var nextButton: Button
    private lateinit var openButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        loadCodes()
    }

    private fun buildUi() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
            gravity = Gravity.TOP
        }
        val title = TextView(this).apply {
            text = "Nusuk Auto Keys"
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
        }
        status = TextView(this).apply { text = "جاري تحميل الأكواد…"; textSize = 15f; setPadding(0, 18, 0, 18) }
        codeView = TextView(this).apply {
            text = "—"; textSize = 22f; gravity = Gravity.CENTER
            setPadding(12, 28, 12, 28)
        }
        openButton = Button(this).apply {
            text = "فتح الكود في نسك"
            isEnabled = false
            setOnClickListener { openCurrentCode() }
        }
        nextButton = Button(this).apply {
            text = "الكود التالي"
            isEnabled = false
            setOnClickListener { chooseNext() }
        }
        val refresh = Button(this).apply {
            text = "تحديث الأكواد"
            setOnClickListener { loadCodes() }
        }
        val note = TextView(this).apply {
            text = "ملاحظة: الموقع مصدر مستقل والأكواد غير مضمونة القبول. التطبيق يفتح رابط الكود المقدم من الموقع؛ لا يتجاوز حدود نسك أو اختبارات التحقق."
            textSize = 13f; setPadding(0, 20, 0, 0)
        }
        layout.addView(title); layout.addView(status); layout.addView(codeView)
        layout.addView(openButton); layout.addView(nextButton); layout.addView(refresh); layout.addView(note)
        setContentView(layout)
    }

    private fun loadCodes() {
        status.text = "جاري جلب أحدث الأكواد من الموقع…"
        openButton.isEnabled = false
        nextButton.isEnabled = false
        thread {
            try {
                val conn = URL(api).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 20000
                conn.setRequestProperty("User-Agent", "NusukAutoKeys/0.1")
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val arr = json.getJSONArray("codes")
                val fresh = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i).optString("code")
                    if (c.startsWith("NG-")) fresh.add(c)
                }
                runOnUiThread {
                    codes.clear(); codes.addAll(fresh); index = 0
                    if (codes.isEmpty()) {
                        status.text = "لم يتم العثور على أكواد."
                    } else {
                        status.text = "تم تحميل ${codes.size} كودًا. المصدر: umrahcodes.click"
                        showCurrent()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "تعذر تحميل الأكواد: ${e.localizedMessage}" }
            }
        }
    }

    private fun showCurrent() {
        if (codes.isEmpty() || index !in codes.indices) return
        codeView.text = codes[index]
        openButton.isEnabled = true
        nextButton.isEnabled = true
        status.text = "الكود ${index + 1} من ${codes.size}"
    }

    private fun openCurrentCode() {
        if (index !in codes.indices) return
        val uri = Uri.parse("https://services.nusuk.sa/app/?p=27&id=${Uri.encode(codes[index])}")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح رابط نسك", Toast.LENGTH_LONG).show()
        }
    }

    private fun chooseNext() {
        if (index + 1 < codes.size) {
            index++
            showCurrent()
        } else {
            status.text = "انتهت الأكواد المحمّلة. حدّث القائمة لاحقًا."
            openButton.isEnabled = false
            nextButton.isEnabled = false
        }
    }
}
