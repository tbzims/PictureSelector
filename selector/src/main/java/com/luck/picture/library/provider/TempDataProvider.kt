package com.luck.picture.library.provider

import android.util.SparseArray
import com.luck.picture.library.entity.LocalMedia
import com.luck.picture.library.entity.LocalMediaAlbum
import com.luck.picture.library.entity.PreviewDataWrap

/**
 * @author：luck
 * @date：2023/6/15 11:09 上午
 * @describe：temporary data provider
 */
class TempDataProvider {

    /**
     * Preview data wrap
     */
    var previewWrap = PreviewDataWrap()

    /**
     * Current Selected album
     */
    var currentMediaAlbum = LocalMediaAlbum.ofDefault()

    /**
     * album data source
     */
    var albumSource = mutableListOf<LocalMediaAlbum>()

    /**
     * media data source
     */
    var mediaSource = mutableListOf<LocalMedia>()


    /**
     * select result
     */
//    var selectResult = mutableListOf<LocalMedia>()

    private var selectResultList = SparseArray<MutableList<LocalMedia>>()

    /**
     * Current apply permission
     */
    var currentRequestPermission = arrayOf<String>()

    fun reset() {
        if (mediaSource.isNotEmpty()) {
            mediaSource.clear()
        }
        if (albumSource.isNotEmpty()) {
            albumSource.clear()
        }
//        if (selectResultList) {
//            selectResult.clear()
//        }
        previewWrap.reset()
        currentMediaAlbum = LocalMediaAlbum.ofDefault()
        if (currentRequestPermission.isNotEmpty()) {
            currentRequestPermission = arrayOf()
        }
    }

    fun getSelectResult(selectResult: Int): MutableList<LocalMedia> {
        return if (selectResultList.get(selectResult) == null) {
            val list = mutableListOf<LocalMedia>()
            selectResultList.put(selectResult, list)
            list
        } else {
            selectResultList.get(selectResult)
        }
    }

    fun destroy(selectResult: Int) {
        selectResultList.remove(selectResult)
    }

    companion object {
        fun getInstance() = InstanceHelper.instance
    }

    object InstanceHelper {
        val instance = TempDataProvider()
    }
}