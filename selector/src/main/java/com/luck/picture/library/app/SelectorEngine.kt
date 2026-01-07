package com.luck.picture.library.app

import com.luck.picture.library.engine.ImageEngine

/**
 * @author：luck
 * @date：2020/4/22 11:36 AM
 * @describe：SelectorEngine
 */
interface SelectorEngine {
    /**
     * Create ImageLoad Engine
     */
    fun createImageLoaderEngine(): ImageEngine
}