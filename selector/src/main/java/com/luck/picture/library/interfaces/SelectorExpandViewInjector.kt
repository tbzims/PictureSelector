package com.luck.picture.library.interfaces

import android.os.Parcel
import android.os.Parcelable
import android.widget.FrameLayout
import com.luck.picture.library.config.SelectorConfig

interface SelectorExpandViewInjector {
    fun inject(viewGroup: FrameLayout?, scope: () -> Unit)

    companion object {

        @JvmField
        val CREATOR = object : Parcelable.Creator<Class<out SelectorExpandViewInjector>> {
            override fun createFromParcel(source: Parcel?): Class<out SelectorExpandViewInjector>? {
                source ?: return null
                val className = source.readString() ?: return null
                return Class.forName(className).asSubclass(SelectorExpandViewInjector::class.java)
            }

            override fun newArray(size: Int): Array<out Class<out SelectorExpandViewInjector>?>? {
                return arrayOfNulls(size)
            }

        }

    }
}