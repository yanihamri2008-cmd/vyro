package com.sonicteam.vyro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonicteam.vyro.data.FeedPost

@Composable
fun Avatar(url: String?, name: String, size: Int = 34) {
    val mod = Modifier.size(size.dp).clip(CircleShape)
    if (url != null) AsyncImage(url, null, mod, contentScale = ContentScale.Crop)
    else Box(mod.background(Brush.linearGradient(listOf(Violet, Magenta))), Alignment.Center) {
        Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.2).sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(p: FeedPost, onLike: () -> Unit, onSave: () -> Unit, onComments: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.avatarUrl, p.username)
            Spacer(Modifier.width(10.dp))
            Text(p.username, fontWeight = FontWeight.SemiBold)
        }
        if (p.media.isNotEmpty()) {
            val pager = rememberPagerState { p.media.size }
            Box {
                HorizontalPager(pager, Modifier.fillMaxWidth().aspectRatio(1f)
                    .pointerInput(p.id) { detectTapGestures(onDoubleTap = { if (!p.liked) onLike() }) }) { i ->
                    AsyncImage(p.media[i], null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                if (p.media.size > 1) Text("${pager.currentPage + 1}/${p.media.size}",
                    Modifier.align(Alignment.TopEnd).padding(10.dp).clip(CircleShape)
                        .background(Color(0x99000000)).padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 12.sp, color = Color.White)
            }
        }
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onLike) {
                if (p.liked) Icon(Icons.Filled.Favorite, "إعجاب", tint = Magenta) else Icon(Icons.Outlined.FavoriteBorder, "إعجاب")
            }
            IconButton(onComments) { Icon(Icons.Outlined.ChatBubbleOutline, "تعليقات") }
            Spacer(Modifier.weight(1f))
            IconButton(onSave) {
                if (p.saved) Icon(Icons.Filled.Bookmark, "حفظ") else Icon(Icons.Outlined.BookmarkBorder, "حفظ")
            }
        }
        Column(Modifier.padding(horizontal = 14.dp)) {
            if (p.likeCount > 0) Text("${p.likeCount} إعجاب", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (!p.caption.isNullOrBlank()) Text("${p.username}  ${p.caption}", fontSize = 14.sp)
            if (p.commentCount > 0) Text("عرض كل التعليقات (${p.commentCount})", Modifier.padding(top = 2.dp),
                color = Color(0xFFA392C7), fontSize = 13.sp)
            Text(p.createdAt.take(10), color = Color(0xFFA392C7), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
        }
    }
}
