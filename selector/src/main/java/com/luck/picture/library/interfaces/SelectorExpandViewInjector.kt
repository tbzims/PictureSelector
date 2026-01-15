package com.luck.picture.library.interfaces

import android.widget.FrameLayout

interface SelectorExpandViewInjector {
    fun inject(viewGroup: FrameLayout, controller: ViewInjectorController)
}