package com.luck.pictureselector

import android.app.Application
import android.content.Context
import com.luck.picture.library.app.IApp
import com.luck.picture.library.app.SelectorAppMaster
import com.luck.picture.library.app.SelectorEngine

class App : Application(), IApp {

    override fun onCreate() {
        super.onCreate()
        SelectorAppMaster.getInstance().setApp(this)
    }

    override fun getAppContext(): Context {
        return this
    }

    override fun getSelectorEngine(): SelectorEngine {
        return SelectorEngineImp()
    }
}