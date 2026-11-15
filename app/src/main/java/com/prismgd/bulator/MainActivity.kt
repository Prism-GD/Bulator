package com.prismgd.bulator

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var terminalOutput: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF101820.toInt())
        }

        val title = TextView(this).apply {
            text = "BULATOR"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
        }

        val subtitle = TextView(this).apply {
            text = "Ubuntu environment for Android"
            textSize = 14f
            setTextColor(0xFFB8C7D1.toInt())
            setPadding(0, 4, 0, 20)
        }

        statusText = TextView(this).apply {
            text = "Status: Ubuntu not installed"
            textSize = 14f
            setTextColor(0xFFFFD166.toInt())
            setPadding(0, 8, 0, 16)
        }

        val terminalTitle = TextView(this).apply {
            text = "TERMINAL"
            textSize = 12f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(0xFF69D2A3.toInt())
        }

        terminalOutput = TextView(this).apply {
            text = "Bulator 0.1.0\\nBase application initialized.\\n\\nUbuntu runtime is not installed yet."
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(0xFFE6EDF3.toInt())
            setPadding(14, 14, 14, 14)
        }

        val terminalPanel = ScrollView(this).apply {
            setBackgroundColor(0xFF080D12.toInt())
            addView(terminalOutput)
        }

        val startButton = Button(this).apply {
            text = "Start Ubuntu"
            isEnabled = false
            contentDescription = "Ubuntu runtime is not implemented yet"
        }

        val note = TextView(this).apply {
            text = "The Ubuntu runtime and PRoot integration will be added in the next development stage."
            textSize = 12f
            setTextColor(0xFF91A4B2.toInt())
            setPadding(0, 12, 0, 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(statusText)
        root.addView(terminalTitle)
        root.addView(terminalPanel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(startButton)
        root.addView(note)

        setContentView(root)
    }
}
