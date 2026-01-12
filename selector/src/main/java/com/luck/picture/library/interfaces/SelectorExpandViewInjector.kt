package com.luck.picture.library.interfaces

import android.os.Parcel
import android.os.Parcelable
import android.widget.FrameLayout
import com.luck.picture.library.config.SelectorConfig

interface SelectorExpandViewInjector {
    fun inject(viewGroup: FrameLayout, scope: () -> Unit)
}