package com.luck.picture.library

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.luck.picture.library.factory.ClassFactory
import com.luck.picture.library.helper.FragmentInjectManager
import com.luck.picture.library.immersive.ImmersiveManager.immersiveAboveAPI23
import com.luck.picture.library.immersive.ImmersiveManager.translucentStatusBar
import com.luck.picture.library.provider.SelectorProviders
import com.tmmtmm.im.style.utils.TmmThemeContext
import com.tmmtmm.im.style.utils.getColorByAttr

/**
 * @author：luck
 * @date：2022/2/10 6:07 下午
 * @describe：SelectorSupporterActivity
 */
class SelectorSupporterActivity : AppCompatActivity() {
    private val config = SelectorProviders.getInstance().getConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(TmmThemeContext.themeResId)

        super.onCreate(savedInstanceState)
        immersive()

        if (SelectorProviders.getInstance().getConfigQueue().isEmpty()) {
            onBackPressedDispatcher.onBackPressed()
            return
        }

        setContentView(R.layout.ps_activity_container)
        val instance = ClassFactory.NewInstance()
            .create(config.registry.get(SelectorMainFragment::class.java))
        FragmentInjectManager.injectFragment(this, instance.getFragmentTag(), instance)
    }

    private fun immersive() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableEdgeToEdge()
            window.insetsController?.let { controller ->
                controller.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )

                controller.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            immersiveAboveAPI23(
                this,
                getColorByAttr(com.tmmtmm.im.style.R.attr.bg_3),
                getColorByAttr(com.tmmtmm.im.style.R.attr.bg_3),
                false
            )
            translucentStatusBar(
                this,
                false
            )
        }
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(
            R.anim.ps_anim_fade_in,
            config.windowAnimStyle.getExitAnimRes()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SelectorSupporterActivity", "onDestroy")
    }
}