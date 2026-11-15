package com.prismgd.bulator

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.content.Intent
import android.provider.Settings
import android.graphics.Typeface
import android.graphics.Color
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("bulator_data", Context.MODE_PRIVATE) }
    private val bg = Color.rgb(16, 24, 32)
    private val panel = Color.rgb(25, 36, 46)
    private val accent = Color.rgb(255, 145, 0)
    private val textColor = Color.rgb(238, 243, 247)
    private val muted = Color.rgb(165, 180, 192)
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var selectedContainer = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureDefaults()
        showHome()
    }

    private fun ensureDefaults() {
        if (!prefs.contains("containers")) {
            val items = JSONArray()
            items.put(JSONObject().apply {
                put("name", "Lubuntu Principal")
                put("distro", "Lubuntu")
                put("resolution", "Automática")
                put("desktop", true)
            })
            prefs.edit().putString("containers", items.toString()).apply()
        }
    }

    private fun containers(): JSONArray = try {
        JSONArray(prefs.getString("containers", "[]"))
    } catch (_: Exception) { JSONArray() }

    private fun saveContainers(items: JSONArray) {
        prefs.edit().putString("containers", items.toString()).apply()
    }

    private fun presets(): JSONArray = try {
        JSONArray(prefs.getString("presets", "[]"))
    } catch (_: Exception) { JSONArray() }

    private fun savePresets(items: JSONArray) {
        prefs.edit().putString("presets", items.toString()).apply()
    }

    private fun baseScreen(title: String, subtitle: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 20)
            setBackgroundColor(bg)
        }
        val heading = TextView(this).apply {
            text = title
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }
        val sub = TextView(this).apply {
            text = subtitle
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 5, 0, 18)
        }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(content) }
        root.addView(heading)
        root.addView(sub)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun label(value: String, size: Float = 15f, color: Int = textColor): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            setPadding(0, 7, 0, 7)
        }

    private fun button(value: String, action: () -> Unit): Button =
        Button(this).apply {
            text = value
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent))
            setOnClickListener { action() }
        }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(14, 12, 14, 12)
        setBackgroundColor(panel)
    }

    private fun showHome() {
        baseScreen("BULATOR", "Lubuntu para Android • ARM64 • sem root")
        content.addView(label("CONTAINERS", 12f, accent))
        val list = containers()
        if (list.length() == 0) content.addView(label("Nenhum container criado.", color = muted))
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            val box = card()
            box.addView(label(item.optString("name", "Container"), 18f))
            val desktopText = if (item.optBoolean("desktop", true)) "LXQt planejado" else "Somente terminal"
            box.addView(label(item.optString("distro", "Lubuntu") + " • " + item.optString("resolution", "Automática") + " • " + desktopText, 12f, muted))
            box.addView(label("Runtime Linux ainda não instalado", 12f, Color.rgb(255, 205, 100)))
            box.addView(button("Selecionar e configurar") {
                selectedContainer = i
                showContainerSettings()
            })
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = 10
            content.addView(box, lp)
        }
        content.addView(button("+ Criar container") { createContainer() })
        content.addView(label("FERRAMENTAS", 12f, accent))
        content.addView(button("Mapeador de teclas") { showKeyMapper() })
        content.addView(button("Meus presets") { showPresets() })
        content.addView(button("Configurações de teclado Android") { openKeyboardSettings() })
        content.addView(label("PRÓXIMA ETAPA", 12f, accent))
        content.addView(label("Integrar PRoot ARM64, instalar o rootfs Lubuntu e conectar o terminal real. Esta versão já salva containers e presets localmente.", 13f, muted))
    }

    private fun createContainer() {
        val input = EditText(this).apply { hint = "Ex.: Lubuntu Testes" }
        AlertDialog.Builder(this)
            .setTitle("Novo container")
            .setMessage("Cada container terá suas próprias configurações.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Criar") { _, _ ->
                val current = containers()
                val name = input.text.toString().trim().ifBlank { "Lubuntu " + (current.length() + 1) }
                current.put(JSONObject().apply {
                    put("name", name)
                    put("distro", "Lubuntu")
                    put("resolution", "Automática")
                    put("desktop", true)
                })
                saveContainers(current)
                selectedContainer = current.length() - 1
                showHome()
            }.show()
    }

    private fun showContainerSettings() {
        val items = containers()
        if (selectedContainer !in 0 until items.length()) return showHome()
        val item = items.getJSONObject(selectedContainer)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }
        val name = EditText(this).apply {
            setText(item.optString("name"))
            hint = "Nome do container"
        }
        val choices = listOf("Automática", "1280 × 720", "1024 × 768", "800 × 600")
        val resolution = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, choices)
            setSelection(choices.indexOf(item.optString("resolution")).coerceAtLeast(0))
        }
        val desktop = CheckBox(this).apply {
            text = "Usar desktop LXQt (quando integrado)"
            setTextColor(textColor)
            isChecked = item.optBoolean("desktop", true)
        }
        box.addView(label("Nome"))
        box.addView(name)
        box.addView(label("Resolução do desktop"))
        box.addView(resolution)
        box.addView(desktop)
        AlertDialog.Builder(this)
            .setTitle("Configurações do container")
            .setView(box)
            .setNegativeButton("Voltar") { _, _ -> showHome() }
            .setNeutralButton("Excluir") { _, _ ->
                val updated = containers()
                val rebuilt = JSONArray()
                for (i in 0 until updated.length()) if (i != selectedContainer) rebuilt.put(updated.getJSONObject(i))
                saveContainers(rebuilt)
                selectedContainer = 0
                showHome()
            }
            .setPositiveButton("Salvar") { _, _ ->
                item.put("name", name.text.toString().trim().ifBlank { "Lubuntu" })
                item.put("resolution", resolution.selectedItem.toString())
                item.put("desktop", desktop.isChecked)
                saveContainers(items)
                showHome()
            }.show()
    }

    private fun showKeyMapper() {
        baseScreen("MAPEADOR DE TECLAS", "Monte um layout e salve como preset.")
        content.addView(label("Editor de teclas", 18f))
        content.addView(label("Digite as teclas separadas por vírgula. Exemplo: W, A, S, D, CTRL, ALT, SHIFT, TAB, ESC, ↑, ↓, ←, →", 13f, muted))
        val editor = EditText(this).apply {
            setText(prefs.getString("current_mapping", "W, A, S, D, CTRL, ALT, SHIFT, TAB, ESC, ↑, ↓, ←, →"))
            setTextColor(textColor)
            setHintTextColor(muted)
            hint = "W, A, S, D..."
            setSingleLine(false)
            minLines = 3
            setPadding(12, 12, 12, 12)
            setBackgroundColor(panel)
        }
        content.addView(editor)
        content.addView(button("Salvar preset") {
            prefs.edit().putString("current_mapping", editor.text.toString()).apply()
            savePresetDialog(editor.text.toString())
        })
        content.addView(button("Abrir teclado Android") { showSoftKeyboard(editor) })
        content.addView(button("Ver presets salvos") { showPresets() })
        content.addView(button("Voltar") { showHome() })
    }

    private fun savePresetDialog(mapping: String) {
        val nameInput = EditText(this).apply { hint = "Nome do preset" }
        AlertDialog.Builder(this)
            .setTitle("Salvar preset")
            .setMessage("As teclas serão salvas neste aparelho.")
            .setView(nameInput)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                val all = presets()
                val presetName = nameInput.text.toString().trim().ifBlank { "Meu preset " + (all.length() + 1) }
                all.put(JSONObject().apply {
                    put("name", presetName)
                    put("mapping", mapping)
                })
                savePresets(all)
                Toast.makeText(this, "Preset salvo!", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun showPresets() {
        val all = presets()
        if (all.length() == 0) {
            AlertDialog.Builder(this).setTitle("Meus presets")
                .setMessage("Você ainda não salvou nenhum preset.")
                .setPositiveButton("OK", null).show()
            return
        }
        val names = Array(all.length()) { i -> all.getJSONObject(i).optString("name", "Preset") }
        AlertDialog.Builder(this).setTitle("Escolha um preset")
            .setItems(names) { _, which ->
                val selected = all.getJSONObject(which)
                prefs.edit().putString("current_mapping", selected.optString("mapping")).apply()
                Toast.makeText(this, "Preset carregado: " + selected.optString("name"), Toast.LENGTH_SHORT).show()
                showKeyMapper()
            }
            .setNegativeButton("Fechar", null).show()
    }

    private fun showSoftKeyboard(target: EditText) {
        target.requestFocus()
        target.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun openKeyboardSettings() {
        try {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, "Abra as configurações de teclado do Android.", Toast.LENGTH_SHORT).show()
        }
    }
}
