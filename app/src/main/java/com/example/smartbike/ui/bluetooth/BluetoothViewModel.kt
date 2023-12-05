package com.example.smartbike.ui.bluetooth

/*
Created By: Daniel Wang
Page Purpose:
 */

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BluetoothViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "This is bluetooth Fragment"
    }
    val text: LiveData<String> = _text
}