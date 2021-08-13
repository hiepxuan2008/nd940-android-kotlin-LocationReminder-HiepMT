package com.udacity.project4.ext

import android.os.Build

inline fun ifSupportsFromAndroidOreo(block: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        block()
    }
}

inline fun ifSupportsFromAndroidQ(block: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        block()
    }
}