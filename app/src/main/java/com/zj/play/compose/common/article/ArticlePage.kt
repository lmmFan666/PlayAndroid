package com.zj.play.compose.common.article

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.blankj.utilcode.util.StringUtils
import com.zj.core.util.getHtmlText
import com.zj.core.util.showToast
import com.zj.model.room.entity.Article
import com.zj.play.R
import com.zj.play.compose.repository.CollectRepository
import com.zj.play.compose.common.BookmarkButton
import com.zj.play.compose.common.PlayAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stateful Article Screen that manages state using produceUiState
 *
 * @param article article
 * @param onBack (event) request back navigation
 */
@Suppress("DEPRECATION") // allow ViewModelLifecycleScope call
@Composable
fun ArticlePage(
    article: Article?,
    onBack: () -> Unit
) {
    // Returns a [CoroutineScope] that is scoped to the lifecycle of [ArticleScreen]. When this
    // screen is removed from composition, the scope will be cancelled.
    ArticleScreen(
        article = article,
        onBack = onBack
    )
}

/**
 * Stateless Article Screen that displays a single post.
 *
 * @param article article
 * @param onBack (event) request navigate back
 */
@Composable
fun ArticleScreen(
    article: Article?,
    onBack: () -> Unit,
) {

    var showDialog by rememberSaveable { mutableStateOf(false) }
    if (showDialog) {
        FunctionalityNotAvailablePopup { showDialog = false }
    }
    val x5WebView = rememberX5WebViewWithLifecycle()
    Scaffold(
        topBar = {
            PlayAppBar(getHtmlText(article?.title ?: "文章详情"), click = {
                if (x5WebView.canGoBack()) {
                    //返回上个页面
                    x5WebView.goBack()
                } else {
                    onBack.invoke()
                }
            })
        },
        content = {
            // Adds view to Compose
            AndroidView(
                { x5WebView },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 56.dp),
            ) { x5WebView ->
                // Reading zoom so that AndroidView recomposes when it changes. The getMapAsync lambda
                // is stored for later, Compose doesn't recognize state reads
                x5WebView.loadUrl(article?.link)
            }
        },
        bottomBar = {
            BottomBar(
                article = article,
                onUnimplementedAction = { showDialog = true },
            )
        }
    )
}

/**
 * Bottom bar for Article screen
 *
 * @param article article
 * @param onUnimplementedAction (event) called when the user performs an unimplemented action
 */
@Composable
private fun BottomBar(
    article: Article?,
    onUnimplementedAction: () -> Unit,
) {

    var favoriteIcon by remember { mutableStateOf(if (article?.collect == false) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite) }
    var loadState by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val collectRepository = CollectRepository()

    Surface(elevation = 2.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
        ) {
            IconButton(onClick = {
                favoriteIcon = if (favoriteIcon == Icons.Filled.Favorite) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val cancelCollects = collectRepository.cancelCollects(article?.id ?: 0)
                        if (cancelCollects.errorCode == 0) {
                            withContext(Dispatchers.Main) {
                                showToast(R.string.collection_cancelled_successfully)
                            }
                        } else {
                            showToast(R.string.failed_to_cancel_collection)
                        }
                    }
                    Icons.Filled.FavoriteBorder
                } else {
                    coroutineScope.launch {
                        val cancelCollects = collectRepository.toCollects(article?.id ?: 0)
                        if (cancelCollects.errorCode == 0) {
                            withContext(Dispatchers.Main) {
                                showToast(R.string.collection_successful)
                            }
                        } else {
                            showToast(R.string.collection_failed)
                        }
                    }
                    Icons.Filled.Favorite
                }
            }) {
                Icon(
                    imageVector = favoriteIcon,
                    contentDescription = "收藏"
                )
            }
            BookmarkButton(
                isBookmarked = loadState,
                onClick = {
                    showToast(if (loadState) "取消书签(假的)" else "添加书签(假的)")
                    loadState = !loadState
                }
            )
            val context = LocalContext.current
            IconButton(onClick = { sharePost(article?.title ?: "", article?.link ?: "", context) }) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "分享"
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onUnimplementedAction) {
                Icon(
                    painter = painterResource(R.drawable.ic_text_settings),
                    contentDescription = "字体设置"
                )
            }
        }
    }
}

/**
 * Display a popup explaining functionality not available.
 *
 * @param onDismiss (event) request the popup be dismissed
 */
@Composable
private fun FunctionalityNotAvailablePopup(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = stringResource(id = R.string.feature_not_available),
                style = MaterialTheme.typography.body2
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.dismiss))
            }
        }
    )
}

/**
 * Show a share sheet for a post
 *
 * @param post to share
 * @param context Android context to show the share sheet in
 */
private fun sharePost(title: String, post: String, context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_TEXT, post)
    }
    context.startActivity(
        Intent.createChooser(
            intent,
            StringUtils.getString(R.string.share_article)
        )
    )
}