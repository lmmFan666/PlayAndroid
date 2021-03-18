package com.zj.play.compose.repository

import com.zj.network.base.PlayAndroidNetwork

/**
 * 版权：Zhujiang 个人版权
 * @author zhujiang
 * 版本：1.5
 * 创建日期：2020/5/19
 * 描述：PlayAndroid
 *
 */
class CollectRepository {

    /**
     * 获取收藏列表
     *
     * @param page 页码
     */
    suspend fun getCollectList(page: Int) = PlayAndroidNetwork.getCollectList(page)

    suspend fun cancelCollects(id: Int) = PlayAndroidNetwork.cancelCollect(id)
    suspend fun toCollects(id: Int) = PlayAndroidNetwork.toCollect(id)

}