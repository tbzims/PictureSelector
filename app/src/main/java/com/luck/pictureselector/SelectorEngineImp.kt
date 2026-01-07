package com.luck.pictureselector

import com.luck.picture.library.app.SelectorEngine
import com.luck.picture.library.engine.ImageEngine

/**
 * @author：luck
 * @date：2020/4/22 12:15 PM
 * @describe：SelectorEngineImp
 */
class SelectorEngineImp : SelectorEngine {
    override fun createImageLoaderEngine(): ImageEngine {
        return GlideEngine.create()
    }
}