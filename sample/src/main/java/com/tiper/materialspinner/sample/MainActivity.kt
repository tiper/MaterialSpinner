package com.tiper.materialspinner.sample

import android.os.Bundle
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
            }
            material_spinner_2.apply {
                adapter = it
                onItemSelectedListener = listener
            }
            material_spinner_3.apply {
                adapter = it
                onItemSelectedListener = listener
                selection = 3
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
            if (material_spinner_1.error.isNullOrEmpty()) {
                material_spinner_1.error = "I am an error"
            } else {
                material_spinner_1.error = null
            }
        }
        b2_clear.setOnClickListener {
            material_spinner_2.selection = ListView.INVALID_POSITION
        }
        b2_error.setOnClickListener {
            if (material_spinner_2.error.isNullOrEmpty()) {
                material_spinner_2.error = "I am an error"
            } else {
                material_spinner_2.error = null
            }
        }
        b3_clear.setOnClickListener {
            material_spinner_3.selection = ListView.INVALID_POSITION
        }
        b3_error.setOnClickListener {
            if (material_spinner_3.error.isNullOrEmpty()) {
                material_spinner_3.error = "I am an error"
            } else {
                material_spinner_3.error = null
            }
        }
    }
}
