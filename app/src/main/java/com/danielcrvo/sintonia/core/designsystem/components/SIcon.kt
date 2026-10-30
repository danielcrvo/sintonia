package com.danielcrvo.sintonia.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

object SIcons {
    val Home = R.drawable.ic_home
    val Tv = R.drawable.ic_tv
    val Film = R.drawable.ic_film
    val Albums = R.drawable.ic_albums
    val Search = R.drawable.ic_search
    val Settings = R.drawable.ic_settings
    val Options = R.drawable.ic_options
    val Star = R.drawable.ic_star
    val StarOutline = R.drawable.ic_star_outline
    val ChevronRight = R.drawable.ic_chevron_right
    val ChevronDown = R.drawable.ic_chevron_down
    val Close = R.drawable.ic_close
    val Check = R.drawable.ic_check
    val Eye = R.drawable.ic_eye
    val EyeOff = R.drawable.ic_eye_off
    val Play = R.drawable.ic_play
    val Pause = R.drawable.ic_pause
    val SkipNext = R.drawable.ic_skip_next
    val SkipPrevious = R.drawable.ic_skip_previous
    val Trash = R.drawable.ic_trash
    val Folder = R.drawable.ic_folder
    val ArrowBack = R.drawable.ic_arrow_back
    val AlertCircle = R.drawable.ic_alert_circle
    val Sun = R.drawable.ic_sun
    val Moon = R.drawable.ic_moon
    val Contrast = R.drawable.ic_contrast
    val Globe = R.drawable.ic_globe
    val Volume = R.drawable.ic_volume
    val Subtitles = R.drawable.ic_subtitles
    val Aspect = R.drawable.ic_aspect
    val Lock = R.drawable.ic_lock
    val Refresh = R.drawable.ic_refresh
    val Add = R.drawable.ic_add
    val List = R.drawable.ic_list
    val Backspace = R.drawable.ic_backspace
    val Document = R.drawable.ic_document
    val Info = R.drawable.ic_info
}

@Composable
fun SIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = SintoniaTheme.colors.text,
    size: Dp = SintoniaTheme.sizes.iconDefault,
) {
    Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
