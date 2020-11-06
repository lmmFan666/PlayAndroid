package com.zj.play.article

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.blankj.utilcode.util.NetworkUtils
import com.bumptech.glide.Glide
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import com.zj.core.Play
import com.zj.core.util.setSafeListener
import com.zj.core.util.showToast
import com.zj.play.R
import com.zj.network.repository.CollectRepository
import com.zj.model.room.PlayDatabase
import com.zj.model.room.entity.Article
import com.zj.model.room.entity.HISTORY
import com.zj.play.main.LoginActivity
import kotlinx.coroutines.*


class ArticleAdapter(
    context: Context,
    articleList: ArrayList<Article>,
    private val isShowCollect: Boolean = true,
    layoutId: Int = R.layout.adapter_article,
) :
    CommonAdapter<Article>(context, layoutId, articleList) {

    private val uiScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun convert(holder: ViewHolder, t: Article, position: Int) {
        val articleLlItem = holder.getView<RelativeLayout>(R.id.articleLlItem)
        val articleIvImg = holder.getView<ImageView>(R.id.articleIvImg)
        val articleTvAuthor = holder.getView<TextView>(R.id.articleTvAuthor)
        val articleTvNew = holder.getView<TextView>(R.id.articleTvNew)
        val articleTvTop = holder.getView<TextView>(R.id.articleTvTop)
        val articleTvTime = holder.getView<TextView>(R.id.articleTvTime)
        val articleTvTitle = holder.getView<TextView>(R.id.articleTvTitle)
        val articleTvChapterName = holder.getView<TextView>(R.id.articleTvChapterName)
        val articleTvCollect = holder.getView<ImageView>(R.id.articleIvCollect)
        if (!TextUtils.isEmpty(t.title))
            articleTvTitle.text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(t.title, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    t.title
                }
        articleTvChapterName.text = t.superChapterName
        articleTvAuthor.text = if (TextUtils.isEmpty(t.author)) t.shareUser else t.author
        articleTvTime.text = t.niceShareDate
        if (!TextUtils.isEmpty(t.envelopePic)) {
            articleIvImg.visibility = View.VISIBLE
            Glide.with(mContext).load(t.envelopePic).into(articleIvImg)
        } else {
            articleIvImg.visibility = View.GONE
        }
        articleTvTop.visibility = if (t.type > 0) View.VISIBLE else View.GONE
        articleTvNew.visibility = if (t.fresh) View.VISIBLE else View.GONE

        articleTvCollect.visibility = if (isShowCollect) View.VISIBLE else View.GONE
        if (t.collect) {
            articleTvCollect.setImageResource(R.drawable.ic_favorite_black_24dp)
        } else {
            articleTvCollect.setImageResource(R.drawable.ic_favorite_border_black_24dp)
        }
        articleTvCollect.setSafeListener {
            if (Play.isLogin) {
                if (NetworkUtils.isConnected()) {
                    t.collect = !t.collect
                    setCollect(t, articleTvCollect)
                } else {
                    showToast(mContext.getString(R.string.no_network))
                }
            } else {
                LoginActivity.actionStart(mContext)
            }
        }
        articleLlItem.setOnClickListener {
            if (!NetworkUtils.isConnected()) {
                showToast(mContext.getString(R.string.no_network))
                return@setOnClickListener
            }
            ArticleActivity.actionStart(
                mContext,
                t.title,
                t.link,
                t.id,
                if (t.collect) 1 else 0,
                userId = t.userId
            )
            val browseHistoryDao = PlayDatabase.getDatabase(mContext).browseHistoryDao()
            uiScope.launch {
                if (browseHistoryDao.getArticle(t.id, HISTORY) == null) {
                    t.localType = HISTORY
                    t.desc = ""
                    browseHistoryDao.insert(t)
                }
            }
        }
    }

    private fun setCollect(t: Article, articleTvCollect: ImageView) {
        val articleDao = PlayDatabase.getDatabase(mContext).browseHistoryDao()
        uiScope.launch {
            if (!t.collect) {
                val cancelCollects = CollectRepository.cancelCollects(t.id)
                if (cancelCollects.errorCode == 0) {
                    withContext(Dispatchers.Main) {
                        articleTvCollect.setImageResource(R.drawable.ic_favorite_border_black_24dp)
                        showToast(mContext.getString(R.string.collection_cancelled_successfully))
                        articleDao.update(t)
                    }
                } else {
                    showToast(mContext.getString(R.string.failed_to_cancel_collection))
                }
            } else {
                val toCollects = CollectRepository.toCollects(t.id)
                if (toCollects.errorCode == 0) {
                    withContext(Dispatchers.Main) {
                        articleTvCollect.setImageResource(R.drawable.ic_favorite_black_24dp)
                        showToast(mContext.getString(R.string.collection_successful))
                        articleDao.update(t)
                    }
                } else {
                    showToast(mContext.getString(R.string.collection_failed))
                }

            }
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

}
