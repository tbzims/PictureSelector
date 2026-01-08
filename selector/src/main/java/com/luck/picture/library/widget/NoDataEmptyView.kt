package com.luck.picture.library.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.luck.picture.library.R
import androidx.core.view.isVisible

class NoDataEmptyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var animationView: LottieAnimationView? = null
    private var tvFirstMessage: TextView? = null
    private var tvSecondMessage: TextView? = null

    init {
        inflate(context, R.layout.view_no_data_empty, this)
        animationView = findViewById(R.id.animationView)
        tvFirstMessage = findViewById(R.id.tvFirstMessage)
        tvSecondMessage = findViewById(R.id.tvSecondMessage)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        when (visibility) {
            VISIBLE -> animationView?.playAnimation()
            GONE, INVISIBLE -> animationView?.pauseAnimation()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isVisible) {
            animationView?.playAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animationView?.pauseAnimation()
    }
}