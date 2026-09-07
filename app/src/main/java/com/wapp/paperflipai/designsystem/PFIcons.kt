package com.wapp.paperflipai.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every icon the app uses, named by role rather than by glyph.
 *
 * iOS reaches for SF Symbols by string at each call site; Android resolves
 * them at compile time, so funnelling them through one object means a
 * rename or a swap happens in exactly one place — and the feature code
 * reads as intent ("PFIcons.Streak") instead of as a glyph name.
 */
object PFIcons {
    // Navigation & chrome
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Forward: ImageVector = Icons.AutoMirrored.Filled.ArrowForward
    val Chevron: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
    val ChevronLeft: ImageVector = Icons.Filled.ChevronLeft
    val Close: ImageVector = Icons.Filled.Close
    val More: ImageVector = Icons.Filled.MoreVert
    val Expand: ImageVector = Icons.Filled.ExpandMore
    val MoveUp: ImageVector = Icons.Filled.KeyboardArrowUp
    val MoveDown: ImageVector = Icons.Filled.KeyboardArrowDown
    val Search: ImageVector = Icons.Filled.Search
    val Refresh: ImageVector = Icons.Filled.Refresh
    val Add: ImageVector = Icons.Filled.Add
    val Check: ImageVector = Icons.Filled.Check
    val CheckCircle: ImageVector = Icons.Filled.CheckCircle
    val Edit: ImageVector = Icons.Filled.Edit
    val Delete: ImageVector = Icons.Filled.Delete
    val Share: ImageVector = Icons.Filled.Share
    val Copy: ImageVector = Icons.Filled.ContentCopy
    val OpenExternal: ImageVector = Icons.Filled.OpenInNew
    val Send: ImageVector = Icons.AutoMirrored.Filled.Send

    // Status
    val Error: ImageVector = Icons.Filled.Error
    val Warning: ImageVector = Icons.Filled.Warning
    val Info: ImageVector = Icons.Filled.Info
    val Offline: ImageVector = Icons.Filled.WifiOff
    val Sync: ImageVector = Icons.Filled.Sync

    // Tabs
    val Library: ImageVector = Icons.Filled.Style
    val Study: ImageVector = Icons.Filled.Psychology
    val Import: ImageVector = Icons.Filled.Add
    val Stats: ImageVector = Icons.Filled.BarChart
    val Settings: ImageVector = Icons.Filled.Settings

    // Content
    val Deck: ImageVector = Icons.Filled.Layers
    val Card: ImageVector = Icons.Filled.Style
    val Folder: ImageVector = Icons.Filled.Folder
    val NewFolder: ImageVector = Icons.Filled.CreateNewFolder
    val Pdf: ImageVector = Icons.Filled.PictureAsPdf
    val Video: ImageVector = Icons.Filled.SmartDisplay
    val Article: ImageVector = Icons.Filled.Newspaper
    val Document: ImageVector = Icons.Filled.Description
    val Upload: ImageVector = Icons.Filled.UploadFile
    val Link: ImageVector = Icons.Filled.Link
    val Public: ImageVector = Icons.Filled.Public
    val Book: ImageVector = Icons.Filled.MenuBook
    val School: ImageVector = Icons.Filled.School

    // Study
    val Streak: ImageVector = Icons.Filled.LocalFireDepartment
    val Timer: ImageVector = Icons.Filled.Timer
    val Trending: ImageVector = Icons.Filled.TrendingUp
    val Calendar: ImageVector = Icons.Filled.CalendarMonth
    val Schedule: ImageVector = Icons.Filled.Schedule
    val Play: ImageVector = Icons.Filled.PlayArrow
    val Idea: ImageVector = Icons.Filled.Lightbulb
    val Sparkle: ImageVector = Icons.Filled.AutoAwesome
    val Bolt: ImageVector = Icons.Filled.Bolt

    // People & projects
    val Person: ImageVector = Icons.Filled.Person
    val PersonAdd: ImageVector = Icons.Filled.PersonAdd
    val Group: ImageVector = Icons.Filled.Groups
    val Verified: ImageVector = Icons.Filled.Verified

    // Auth & account
    val Email: ImageVector = Icons.Filled.Email
    val Password: ImageVector = Icons.Filled.Lock
    val Reveal: ImageVector = Icons.Filled.Visibility
    val Hide: ImageVector = Icons.Filled.VisibilityOff
    val SignOut: ImageVector = Icons.AutoMirrored.Filled.Logout

    // Settings
    val Appearance: ImageVector = Icons.Filled.DarkMode
    val LightMode: ImageVector = Icons.Filled.LightMode
    val System: ImageVector = Icons.Filled.PhoneAndroid
    val Language: ImageVector = Icons.Filled.Language
    val Notifications: ImageVector = Icons.Filled.Notifications
    val NotificationsOff: ImageVector = Icons.Filled.NotificationsOff
    val Support: ImageVector = Icons.Filled.SupportAgent
    val Help: ImageVector = Icons.Filled.HelpOutline
    val Pro: ImageVector = Icons.Filled.WorkspacePremium
    val Star: ImageVector = Icons.Filled.Star
    val Flag: ImageVector = Icons.Filled.Flag

    // View modes
    val Grid: ImageVector = Icons.Filled.GridView
    val List: ImageVector = Icons.Filled.ViewList
}
