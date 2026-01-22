package com.luck.picture.library

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.luck.picture.library.factory.ClassFactory
import com.luck.picture.library.helper.FragmentInjectManager
import com.luck.picture.library.provider.SelectorProviders
import com.tmmtmm.im.style.utils.TmmThemeContext

/**
 * @author：luck
 * @date：2022/2/10 6:07 下午
 * @describe：SelectorTransparentActivity
 */
class SelectorTransparentActivity : AppCompatActivity() {
    private val config = SelectorProviders.getInstance().getConfig()
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(TmmThemeContext.themeResId)
        super.onCreate(savedInstanceState)
        immersive()
        setActivitySize()
        setContentView(R.layout.ps_empty)
        val registry = config.registry
        val factory = ClassFactory.NewInstance()
        when {
            config.previewWrap.isExternalPreview -> {
                var instance = factory.create(SelectorPreviewFragment::class.java)
                if (instance::class.java.isAssignableFrom(SelectorPreviewFragment::class.java)) {
                    // No custom registry, use default external preview component
                    instance =
                        factory.create(registry.get(SelectorExternalPreviewFragment::class.java))
                }
                FragmentInjectManager.injectFragment(this, instance.getFragmentTag(), instance)
            }

            config.systemGallery -> {
                val instance = factory.create(SelectorSystemFragment::class.java)
                FragmentInjectManager.injectFragment(this, instance.getFragmentTag(), instance)
            }

            else -> {
                // Only Using Camera Fragment
                val instance = factory.create(registry.get(SelectorCameraFragment::class.java))
                FragmentInjectManager.injectFragment(this, instance.getFragmentTag(), instance)
            }
        }
    }

    private fun immersive() {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
//        ImmersiveManager.immersiveAboveAPI23(
//            this,
//            config.statusBarStyle.getStatusBarColor(),
//            config.statusBarStyle.getNavigationBarColor(),
//            config.statusBarStyle.isDarkStatusBar()
//        )
    }

    private fun setActivitySize() {
        if (config.isOnlyCamera || config.systemGallery) {
            window.setGravity(Gravity.LEFT or Gravity.TOP)
            val params = window.attributes
            params.x = 0
            params.y = 0
            params.height = 1
            params.width = 1
            window.attributes = params
        }
    }

    override fun finish() {
        super.finish()
        if ((config.previewWrap.isExternalPreview && !config.isPreviewZoomEffect)) {
            overridePendingTransition(
                R.anim.ps_anim_fade_in,
                config.windowAnimStyle.getExitAnimRes()
            )
        } else {
            if (!config.isOnlyCamera && !config.systemGallery) {
                overridePendingTransition(0, R.anim.ps_anim_fade_out)
            }
        }
    }
}