package com.tiper.materialspinner.sample

import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import com.tiper.MaterialSpinner
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val listener by lazy {
        object : MaterialSpinner.OnItemSelectedListener {
            override fun onItemSelected(parent: MaterialSpinner, view: View?, position: Int, id: Long) {
                Log.v("MaterialSpinner", "onItemSelected parent=${parent.id}, position=$position")
                parent.focusSearch(View.FOCUS_UP)?.requestFocus()
            }

            override fun onNothingSelected(parent: MaterialSpinner) {
                Log.v("MaterialSpinner", "onNothingSelected parent=${parent.id}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ArrayAdapter.createFromResource(this, R.array.planets_array, android.R.layout.simple_spinner_item).let {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            material_spinner_1.apply {
                adapter = it
                onItemSelectedListener = listener
                onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    Log.v("MaterialSpinner", "onFocusChange hasFocus=$hasFocus")
                }
            }
            material_spinner_2.apply {
                adapter = it
                onItemSelectedListener = listener
            }
            material_spinner_3.apply {
                adapter = it
                onItemSelectedListener = listener
                selection = 3
                setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_arrow_downward, theme))
            }
            spinner.adapter = it
            appCompatSpinner.adapter = it
        }
        material_spinner_1.let {

        }
        b1_clear.setOnClickListener {
            material_spinner_1.selection = ListView.INVALID_POSITION
        }
        b1_error.setOnClickListener {
            material_spinner_1.onClick()
        }
        b1_click.setOnClickListener {
            material_spinner_1.performClick()
        }
        b2_clear.setOnClickListener {
            material_spinner_2.selection = ListView.INVALID_POSITION
        }
        b2_error.setOnClickListener {
            material_spinner_2.onClick()
        }
        b2_click.setOnClickListener {
            material_spinner_2.performClick()
        }
        b3_clear.setOnClickListener {
            material_spinner_3.selection = ListView.INVALID_POSITION
        }
        b3_error.setOnClickListener {
            material_spinner_3.onClick()
        }
        b3_click.setOnClickListener {
            material_spinner_3.performClick()
        }
    }

    private fun MaterialSpinner.onClick() {
        error = if (error.isNullOrEmpty()) resources.getText(R.string.error) else null
    }
}
