@file:OptIn(UnstableApi::class)

package com.sonicteam.vyro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.sonicteam.vyro.data.ReelItem
import com.sonicteam.vyro.data.Repo
import com.sonicteam.vyro.data.friendly
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val reelReasons = listOf("محتوى مسيء", "احتيال أو بريد مزعج", "عنف", "انتهاك حقوق", "أخرى")

@Composable
private fun ReelPlayer(url: String, paused: Boolean, modifier: Modifier) {
    val ctx = LocalContext.current
    val exo = remember(url) {
        ExoPlayer.Builder(ctx).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }
    DisposableEffect(exo) { onDispose { exo.release() } }
    LaunchedEffect(paused, exo) { exo.playWhenReady = !paused }
    AndroidView(
        factory = { c ->
            PlayerView(c).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = exo
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ReelPage(
    r: ReelItem, active: Boolean,
    onLike: () -> Unit, onComments: () -> Unit, onProfile: () -> Unit, onMenu: () -> Unit, mine: Boolean
) {
    var paused by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val tap = Modifier.fillMaxSize().pointerInput(r.id) {
            detectTapGestures(onTap = { paused = !paused }, onDoubleTap = { if (!r.liked) onLike() })
        }
        if (active) ReelPlayer(r.videoUrl, paused, tap)
        else AsyncImage(r.thumbUrl, null, tap, contentScale = ContentScale.Crop)
        if (active && paused) Icon(Icons.Filled.PlayArrow, null, Modifier.align(Alignment.Center).size(72.dp), tint = Color(0xCCFFFFFF))

        Box(Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))

        Column(Modifier.align(Alignment.BottomStart).padding(16.dp).padding(end = 84.dp)) {
            Row(Modifier.clickable { onProfile() }, verticalAlignment = Alignment.CenterVertically) {
                Avatar(r.avatarUrl, r.username, 36)
                Spacer(Modifier.width(10.dp))
                Text(r.username, color = Color.White, fontWeight = FontWeight.Bold)
            }
            if (!r.caption.isNullOrBlank()) Text(r.caption, color = Color.White, fontSize = 14.sp, maxLines = 3, modifier = Modifier.padding(top = 8.dp))
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onLike) {
                if (r.liked) Icon(Icons.Filled.Favorite, "إعجاب", Modifier.size(32.dp), tint = Magenta)
                else Icon(Icons.Outlined.FavoriteBorder, "إعجاب", Modifier.size(32.dp), tint = Color.White)
            }
            Text("${r.likeCount}", color = Color.White, fontSize = 12.sp)
            IconButton(onComments) { Icon(Icons.Outlined.ChatBubbleOutline, "تعليقات", Modifier.size(30.dp), tint = Color.White) }
            Text("${r.commentCount}", color = Color.White, fontSize = 12.sp)
            Box {
                IconButton({ menu = true }) { Icon(Icons.Filled.MoreVert, "المزيد", tint = Color.White) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(if (mine) "حذف الريل" else "إبلاغ") }, onClick = { menu = false; onMenu() })
                }
            }
        }
    }
}

@Composable
fun ReelsScreen(reloadKey: Int, onAdd: () -> Unit, onProfile: (String) -> Unit, onNotice: (String) -> Unit) {
    var reels by remember { mutableStateOf<List<ReelItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }
    var commentsFor by remember { mutableStateOf<ReelItem?>(null) }
    var reportFor by remember { mutableStateOf<ReelItem?>(null) }
    val scope = rememberCoroutineScope()
    val me = Repo.uid

    LaunchedEffect(reloadKey) {
        loading = true
        try { reels = Repo.reels(); err = null }
        catch (e: CancellationException) { throw e } catch (e: Exception) { err = e.friendly() }
        loading = false
    }
    fun update(id: String, f: (ReelItem) -> ReelItem) { reels = reels.map { if (it.id == id) f(it) else it } }
    fun toggleLike(r: ReelItem) {
        val on = !r.liked
        update(r.id) { it.copy(liked = on, likeCount = it.likeCount + if (on) 1 else -1) }
        scope.launch { try { Repo.setReelLiked(r.id, on) } catch (e: Exception) { update(r.id) { r } } }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (reels.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                if (loading) CircularProgressIndicator(color = Color.White)
                else {
                    Text(err ?: "لا توجد ريلز بعد. انشر أول ريل!", color = if (err != null) Color(0xFFFF6B8A) else Color.White)
                }
            }
        } else {
            val pager = rememberPagerState { reels.size }
            VerticalPager(pager, Modifier.fillMaxSize(), key = { reels[it].id }) { page ->
                val r = reels[page]
                ReelPage(
                    r, active = pager.currentPage == page,
                    onLike = { toggleLike(r) }, onComments = { commentsFor = r },
                    onProfile = { onProfile(r.authorId) }, mine = r.authorId == me,
                    onMenu = {
                        if (r.authorId == me) scope.launch {
                            try { Repo.deleteReel(r.id); reels = reels.filter { it.id != r.id }; onNotice("تم حذف الريل") }
                            catch (e: Exception) { onNotice(e.friendly()) }
                        } else reportFor = r
                    })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ريلز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
            IconButton(onAdd) { Icon(Icons.Filled.Add, "ريل جديد", Modifier.size(30.dp), tint = Color.White) }
        }
    }

    commentsFor?.let { r ->
        ReelCommentsSheet(r, onDismiss = { commentsFor = null },
            onAdded = { update(r.id) { it.copy(commentCount = it.commentCount + 1) } })
    }
    reportFor?.let { r ->
        AlertDialog(
            onDismissRequest = { reportFor = null },
            title = { Text("سبب الإبلاغ") },
            text = {
                Column {
                    reelReasons.forEach { why ->
                        TextButton({
                            reportFor = null
                            scope.launch {
                                try { Repo.report("reel", r.id, why); onNotice("تم إرسال البلاغ. شكراً لك.") }
                                catch (e: Exception) { onNotice("تعذّر إرسال البلاغ") }
                            }
                        }, Modifier.fillMaxWidth()) { Text(why) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ reportFor = null }) { Text("إلغاء") } }
        )
    }
}
