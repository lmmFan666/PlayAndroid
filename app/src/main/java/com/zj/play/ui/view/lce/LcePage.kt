package com.zj.play.ui.view.lce

import androidx.compose.runtime.Composable
import com.zj.model.PlayError
import com.zj.model.PlayLoading
import com.zj.model.PlayState
import com.zj.model.PlaySuccess
import com.zj.utils.XLog


@Composable
fun <T> LcePage(
    playState: PlayState<T>,
    onErrorClick: () -> Unit,
    content: @Composable (data: T) -> Unit
) {
    when (playState) {
        is PlayLoading -> {
            LoadingContent()
        }

        is PlayError -> {
            ErrorContent(onErrorClick = onErrorClick)
        }

        is PlaySuccess<T> -> {
            content(playState.data)
        }

        else -> {
            XLog.i()
        }
    }
}