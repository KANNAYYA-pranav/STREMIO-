package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.database.DownloadEntity
import com.example.database.PlaybackProgressEntity
import com.example.database.ProfileEntity
import com.example.model.MediaItem
import com.example.model.MovieCatalog
import com.example.ui.theme.*
import com.example.viewmodel.StreamViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin
import kotlin.math.exp
import kotlin.math.PI

@Composable
fun NetStreamApp(viewModel: StreamViewModel) {
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    var showIntro by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NetstreamPureBlack
    ) {
        if (showIntro) {
            NetStreamIntroScreen(
                onFinished = { showIntro = false }
            )
        } else {
            Crossfade(targetState = selectedProfile, label = "ProfileHubCrossfade") { profile ->
                if (profile == null) {
                    ProfileSelectionScreen(
                        profiles = profiles,
                        onProfileSelected = { viewModel.selectProfile(it) },
                        onAddProfile = { name, color, isKids -> viewModel.createProfile(name, color, isKids) },
                        onDeleteProfile = { viewModel.deleteProfile(it) }
                    )
                } else {
                    MainHubNavigation(
                        viewModel = viewModel,
                        activeProfile = profile,
                        onSignOutProfile = { viewModel.selectProfile(null) }
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. PROFILE SELECTION SCREEN
// ==========================================
@Composable
fun ProfileSelectionScreen(
    profiles: List<ProfileEntity>,
    onProfileSelected: (ProfileEntity) -> Unit,
    onAddProfile: (String, String, Boolean) -> Unit,
    onDeleteProfile: (ProfileEntity) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var isDeleteMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1F0305),
                        NetstreamPureBlack,
                        NetstreamPureBlack
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // NetStream Signature Red Header
            Text(
                text = "NETSTREAM",
                color = NetstreamRed,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 6.sp,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .testTag("app_logo_title")
            )

            Text(
                text = "Who's watching?",
                color = NetstreamWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Profiles Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                maxItemsInEachRow = 2
            ) {
                profiles.forEach { profile ->
                    ProfileCardItem(
                        profile = profile,
                        isDeleteMode = isDeleteMode,
                        onClicked = {
                            if (isDeleteMode) {
                                onDeleteProfile(profile)
                            } else {
                                onProfileSelected(profile)
                            }
                        }
                    )
                }

                if (profiles.size < 5) {
                    AddNewProfileCard(onClick = { showCreateDialog = true })
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Edit Profile Button
            Button(
                onClick = { isDeleteMode = !isDeleteMode },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDeleteMode) NetstreamRed else Color.Transparent,
                    contentColor = NetstreamWhite
                ),
                border = BorderStroke(1.dp, if (isDeleteMode) Color.Transparent else NetstreamLightGrey),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(180.dp)
                    .testTag("edit_profiles_button")
            ) {
                Icon(
                    imageName(isDeleteMode),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDeleteMode) "Done" else "Manage Profiles",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, color, isKids ->
                onAddProfile(name, color, isKids)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun inlineColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        NetstreamRed
    }
}

@Composable
fun imageName(isDeleteMode: Boolean) = if (isDeleteMode) Icons.Default.Check else Icons.Default.Edit

@Composable
fun ProfileCardItem(
    profile: ProfileEntity,
    isDeleteMode: Boolean,
    onClicked: () -> Unit
) {
    val color = inlineColor(profile.avatarColorHex)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .width(100.dp)
            .clickable { onClicked() }
            .testTag("profile_item_${profile.name.lowercase()}")
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .border(2.dp, if (isDeleteMode) NetstreamRed else Color.Transparent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Smiley / Initials for placeholder profile avatar
            Text(
                text = profile.name.take(1).uppercase(),
                color = NetstreamWhite,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )

            if (isDeleteMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Profile",
                        tint = NetstreamRed,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = profile.name,
            color = NetstreamWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (profile.isKids) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(NetstreamRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "KIDS",
                    color = NetstreamRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AddNewProfileCard(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .width(100.dp)
            .clickable { onClick() }
            .testTag("add_profile_card")
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NetstreamGrey)
                .border(1.dp, NetstreamBorderGrey, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Profile",
                tint = NetstreamLightGrey,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Add Profile",
            color = NetstreamLightGrey,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#E50914") }
    var isKidsMode by remember { mutableStateOf(false) }

    val colorsList = listOf(
        "#E50914", // Signature Red
        "#0080FF", // Blue
        "#32CD32", // Green
        "#FFD700", // Gold
        "#9400D3", // Purple
        "#FF69B4"  // Pink
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NetstreamGrey),
            modifier = Modifier
                .border(1.dp, NetstreamBorderGrey, RoundedCornerShape(16.dp))
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Profile",
                    color = NetstreamWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Profile Avatar Placeholder Preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(android.graphics.Color.parseColor(selectedColor))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (profileName.isNotEmpty()) profileName.take(1).uppercase() else "N",
                        color = NetstreamWhite,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name", color = NetstreamLightGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NetstreamWhite,
                        unfocusedTextColor = NetstreamWhite,
                        focusedBorderColor = NetstreamRed,
                        unfocusedBorderColor = NetstreamBorderGrey
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_profile_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Colors Selector
                Text(
                    text = "Choose Profile Theme Color",
                    color = NetstreamWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorsList.forEach { col ->
                        val parsedCol = Color(android.graphics.Color.parseColor(col))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedCol)
                                .border(
                                    width = if (selectedColor == col) 3.dp else 0.dp,
                                    color = if (selectedColor == col) NetstreamWhite else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = col }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kids Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "For Kids (Filters content)",
                        color = NetstreamWhite,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = isKidsMode,
                        onCheckedChange = { isKidsMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NetstreamWhite,
                            checkedTrackColor = NetstreamRed,
                            uncheckedThumbColor = NetstreamLightGrey,
                            uncheckedTrackColor = NetstreamBorderGrey
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Confirm buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = NetstreamLightGrey)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { if (profileName.isNotBlank()) onConfirm(profileName, selectedColor, isKidsMode) },
                        colors = ButtonDefaults.buttonColors(containerColor = NetstreamRed)
                    ) {
                        Text("Create", color = NetstreamWhite)
                    }
                }
            }
        }
    }
}

// FlowRow layout simulator
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    maxItemsInEachRow: Int = 3,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = horizontalArrangement
            ) {
                // Implement very simple structural wraps
                content()
            }
        }
    }
}

// ==========================================
// 2. MAIN HUB NAVIGATION & SCAFFOLD
// ==========================================
@Composable
fun MainHubNavigation(
    viewModel: StreamViewModel,
    activeProfile: ProfileEntity,
    onSignOutProfile: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("home") }
    var activeStreamingMovie by remember { mutableStateOf<MediaItem?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NetstreamBlack,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            ) {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetstreamRed,
                        selectedTextColor = NetstreamWhite,
                        unselectedIconColor = NetstreamLightGrey,
                        unselectedTextColor = NetstreamLightGrey,
                        indicatorColor = Color(0x33E50914)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "search",
                    onClick = { selectedTab = "search" },
                    icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search & AI") },
                    label = { Text("AI Concierge", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetstreamRed,
                        selectedTextColor = NetstreamWhite,
                        unselectedIconColor = NetstreamLightGrey,
                        unselectedTextColor = NetstreamLightGrey,
                        indicatorColor = Color(0x33E50914)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "downloads",
                    onClick = { selectedTab = "downloads" },
                    icon = { Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Downloads") },
                    label = { Text("Downloads", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetstreamRed,
                        selectedTextColor = NetstreamWhite,
                        unselectedIconColor = NetstreamLightGrey,
                        unselectedTextColor = NetstreamLightGrey,
                        indicatorColor = Color(0x33E50914)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == "devices",
                    onClick = { selectedTab = "devices" },
                    icon = { Icon(imageVector = Icons.Default.Devices, contentDescription = "Devices & List") },
                    label = { Text("Watchlist", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetstreamRed,
                        selectedTextColor = NetstreamWhite,
                        unselectedIconColor = NetstreamLightGrey,
                        unselectedTextColor = NetstreamLightGrey,
                        indicatorColor = Color(0x33E50914)
                    )
                )
            }
        },
        containerColor = NetstreamPureBlack,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    activeProfile = activeProfile,
                    onMediaPlayTrigger = { activeStreamingMovie = it },
                    onSignOutProfile = onSignOutProfile
                )
                "search" -> SearchAndAIConciergeScreen(
                    viewModel = viewModel,
                    activeProfile = activeProfile,
                    onMediaPlayTrigger = { activeStreamingMovie = it }
                )
                "downloads" -> DownloadsScreen(
                    viewModel = viewModel,
                    onMediaPlayTrigger = { activeStreamingMovie = it }
                )
                "devices" -> DevicesAndWatchlistScreen(
                    viewModel = viewModel,
                    onMediaPlayTrigger = { activeStreamingMovie = it }
                )
            }
        }
    }

    // Media Streaming Player sliding Overlay
    if (activeStreamingMovie != null) {
        UltraHDMediaPlayer(
            mediaItem = activeStreamingMovie!!,
            viewModel = viewModel,
            onClosePlayer = { activeStreamingMovie = null }
        )
    }
}

// ==========================================
// 3. HOME COVER SCREEN
// ==========================================
@Composable
fun HomeScreen(
    viewModel: StreamViewModel,
    activeProfile: ProfileEntity,
    onMediaPlayTrigger: (MediaItem) -> Unit,
    onSignOutProfile: () -> Unit
) {
    val allMovies by viewModel.allMediaItems.collectAsStateWithLifecycle()
    val billboardMovie = remember(allMovies) {
        allMovies.firstOrNull { it.isBillboard } ?: allMovies.firstOrNull() ?: com.example.model.MovieCatalog.items.first()
    }
    val watchlistItems by viewModel.watchlist.collectAsStateWithLifecycle()
    val progressItems by viewModel.allProgress.collectAsStateWithLifecycle()

    var selectedDetailMovie by remember { mutableStateOf<MediaItem?>(null) }
    var scaffoldScrollState = rememberScrollState()

    var isBillboardAddedToWatchlist by remember { mutableStateOf(false) }
    var showUploadForm by remember { mutableStateOf(false) }

    LaunchedEffect(watchlistItems, billboardMovie) {
        isBillboardAddedToWatchlist = watchlistItems.any { it.mediaId == billboardMovie.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetstreamPureBlack)
            .verticalScroll(scaffoldScrollState)
    ) {
        // 1. BRAND HEADER (Top Navigation matching High Density HTML layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Signature Netstream Logo N
                Text(
                    text = "N",
                    color = NetstreamRed,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(end = 4.dp)
                )
                
                // Active Home Tab Indicator matching border-b-2 border-red-600 pb-1
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Home",
                        color = NetstreamWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(NetstreamRed)
                    )
                }

                Text(
                    text = "TV Shows",
                    color = NetstreamLightGrey,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { }
                )

                Text(
                    text = "Movies",
                    color = NetstreamLightGrey,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { }
                )

                Text(
                    text = "Upload",
                    color = NetstreamLightGrey,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { showUploadForm = true }
                        .testTag("upload_tab_button")
                )
            }

            // Profile switcher capsule in top right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable { onSignOutProfile() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(inlineColor(activeProfile.avatarColorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeProfile.name.take(1).uppercase(),
                        color = NetstreamWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Profiles",
                    color = NetstreamWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Switch Profile",
                    tint = NetstreamLightGrey,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // 2. HERO BILLBOARD CARD (curved, elegant card with dual overlays)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NetstreamCardDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Poster
                AsyncImage(
                    model = billboardMovie.backdropUrl,
                    contentDescription = billboardMovie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Advanced dual-gradient shading overlay (gradient-to-t & gradient-to-b)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Bottom Metadata & Action Buttons from spec
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = billboardMovie.title.uppercase(),
                        color = NetstreamWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge.copy(
                            shadow = Shadow(color = Color.Black, offset = Offset(1f, 2f), blurRadius = 4f)
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = billboardMovie.genre.split(" • ").joinToString("  •  ").uppercase(),
                        color = NetstreamLightGrey,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Side-by-side capsule action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onMediaPlayTrigger(billboardMovie) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetstreamWhite,
                                contentColor = NetstreamPureBlack
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("billboard_play_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // My List toggle button
                        Button(
                            onClick = { viewModel.toggleWatchlist(billboardMovie.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = NetstreamWhite
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isBillboardAddedToWatchlist) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "My List",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("My List", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. HORIZONTAL SCROLL ROWS
        // A. Continue Watching
        if (progressItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Continue Watching for ${activeProfile.name}",
                    color = NetstreamWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                items(progressItems) { prog ->
                    val movieMatch = allMovies.find { it.id == prog.mediaId } ?: billboardMovie
                    ContinueWatchingItem(
                        progress = prog,
                        mediaItem = movieMatch,
                        onPlay = { onMediaPlayTrigger(it) },
                        onInfo = { selectedDetailMovie = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // B. Trending Now Row with special TOP 10 badges
        MovieRowSection(
            title = "Trending Now",
            items = viewModel.getMoviesByCategory("trending", allMovies),
            onMovieClick = { selectedDetailMovie = it },
            showTopBadgeIndices = true
        )

        // C. Cyberpunk Hits
        MovieRowSection(
            title = "Cyberpunk Hits",
            items = viewModel.getMoviesByCategory("cyberpunk", allMovies),
            onMovieClick = { selectedDetailMovie = it }
        )

        // D. Science Fiction
        MovieRowSection(
            title = "Award-Winning Science Fiction",
            items = viewModel.getMoviesByCategory("scifi", allMovies),
            onMovieClick = { selectedDetailMovie = it }
        )

        // E. Action Anime Chronicles
        MovieRowSection(
            title = "Action Anime Chronicles",
            items = viewModel.getMoviesByCategory("anime", allMovies),
            onMovieClick = { selectedDetailMovie = it }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (selectedDetailMovie != null) {
        MovieDetailOverlay(
            mediaItem = selectedDetailMovie!!,
            viewModel = viewModel,
            onDismiss = { selectedDetailMovie = null },
            onPlay = {
                onMediaPlayTrigger(it)
                selectedDetailMovie = null
            }
        )
    }

    if (showUploadForm) {
        UploadMovieDialog(
            onDismiss = { showUploadForm = false },
            viewModel = viewModel
        )
    }
}

@Composable
fun ContinueWatchingItem(
    progress: PlaybackProgressEntity,
    mediaItem: MediaItem,
    onPlay: (MediaItem) -> Unit,
    onInfo: (MediaItem) -> Unit
) {
    val progressFraction = progress.progressMs.toFloat() / progress.durationMs.coerceAtLeast(1L)

    Card(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                ) {
                    AsyncImage(
                        model = mediaItem.imageUrl,
                        contentDescription = mediaItem.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay small play button in center
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable { onPlay(mediaItem) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play icon",
                                tint = NetstreamPureBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Progress Bar lines
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = NetstreamRed,
                    trackColor = NetstreamLightGrey.copy(alpha = 0.3f),
                )

                // Title and info trigger row matching high density
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaItem.title,
                            color = NetstreamWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "S1:E4 \"The Horizon\"",
                            color = NetstreamLightGrey,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onInfo(mediaItem) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = NetstreamLightGrey,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MovieRowSection(
    title: String,
    items: List<MediaItem>,
    onMovieClick: (MediaItem) -> Unit,
    showTopBadgeIndices: Boolean = false
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = NetstreamWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "See All",
                tint = NetstreamLightGrey,
                modifier = Modifier.size(20.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items.size) { index ->
                val item = items[index]
                val hasBadge = showTopBadgeIndices && (index < 3)
                MoviePosterCard(
                    mediaItem = item,
                    showTopBadge = hasBadge,
                    onClick = { onMovieClick(item) }
                )
            }
        }
    }
}

@Composable
fun MoviePosterCard(
    mediaItem: MediaItem,
    showTopBadge: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(160.dp)
            .clickable { onClick() }
            .testTag("movie_poster_${mediaItem.id}")
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = NetstreamGrey),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = mediaItem.imageUrl,
                contentDescription = mediaItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (showTopBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xFFE50914), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TOP 10",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. MOVIE DETAIL MODAL DIALOG
// ==========================================
@Composable
fun MovieDetailOverlay(
    mediaItem: MediaItem,
    viewModel: StreamViewModel,
    onDismiss: () -> Unit,
    onPlay: (MediaItem) -> Unit
) {
    val watchlistItems by viewModel.watchlist.collectAsStateWithLifecycle()
    val downloadsList by viewModel.downloads.collectAsStateWithLifecycle()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsStateWithLifecycle()

    var isSavedInWatchlist by remember { mutableStateOf(false) }
    var isDownloaded by remember { mutableStateOf(false) }
    val isDownloading = downloadProgressMap.containsKey(mediaItem.id)
    val activeDLProgress = downloadProgressMap[mediaItem.id] ?: 0

    LaunchedEffect(watchlistItems) {
        isSavedInWatchlist = watchlistItems.any { it.mediaId == mediaItem.id }
    }

    LaunchedEffect(downloadsList) {
        isDownloaded = downloadsList.any { it.mediaId == mediaItem.id && it.isCompleted }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NetstreamCardDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NetstreamBorderGrey, RoundedCornerShape(12.dp))
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                // Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AsyncImage(
                        model = mediaItem.backdropUrl,
                        contentDescription = "Backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, NetstreamCardDark)
                                )
                            )
                    )

                    // Close circle top-right
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NetstreamWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Information content
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = mediaItem.title,
                        color = NetstreamWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Specs row (year, rating, duration)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "${mediaItem.matchScore}% Match",
                            color = Color(0xFF46D369),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = mediaItem.year.toString(),
                            color = NetstreamLightGrey,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .background(NetstreamBorderGrey, RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = mediaItem.rating,
                                color = NetstreamWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = mediaItem.duration,
                            color = NetstreamLightGrey,
                            fontSize = 13.sp
                        )
                    }

                    // Solid Play trigger button
                    Button(
                        onClick = { onPlay(mediaItem) },
                        colors = ButtonDefaults.buttonColors(containerColor = NetstreamRed),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("detail_play_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play details",
                            tint = NetstreamWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stream in High-Definition", fontWeight = FontWeight.Bold)
                    }

                    // Description text
                    Text(
                        text = mediaItem.description,
                        color = NetstreamWhite.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Metadata attributes (Genre, Cast info)
                    Text(
                        text = "Genre: " + mediaItem.genre,
                        color = NetstreamLightGrey,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Creators: NetStream Cinematic Studios",
                        color = NetstreamLightGrey,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Secondary action buttons (My List, Download)
                    Divider(color = NetstreamBorderGrey, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Watchlist toggle
                        IconButtonWithText(
                            icon = if (isSavedInWatchlist) Icons.Default.Check else Icons.Default.Add,
                            text = if (isSavedInWatchlist) "In List" else "My List",
                            active = isSavedInWatchlist,
                            onClicked = { viewModel.toggleWatchlist(mediaItem.id) }
                        )

                        // Download status toggle
                        IconButtonWithText(
                            icon = when {
                                isDownloaded -> Icons.Default.FileDownloadDone
                                isDownloading -> Icons.Default.Cached
                                else -> Icons.Default.FileDownload
                            },
                            text = when {
                                isDownloaded -> "Downloaded"
                                isDownloading -> "$activeDLProgress%"
                                else -> "Download"
                            },
                            active = isDownloaded || isDownloading,
                            onClicked = {
                                if (!isDownloaded && !isDownloading) {
                                    viewModel.downloadMovie(mediaItem)
                                } else if (isDownloaded) {
                                    viewModel.deleteDownload(mediaItem.id)
                                }
                            }
                        )

                        // If user-uploaded, offer dynamic Room delete
                        if (mediaItem.id.startsWith("custom_")) {
                            IconButtonWithText(
                                icon = Icons.Default.Delete,
                                text = "Delete Custom",
                                active = true,
                                onClicked = {
                                    viewModel.deleteCustomMovie(mediaItem.id)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconButtonWithText(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    active: Boolean,
    onClicked: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClicked() }
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (active) NetstreamRed else NetstreamWhite,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            color = if (active) NetstreamRed else NetstreamLightGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// 5. SEARCH & AI CONCIERGE SCREEN
// ==========================================
@Composable
fun SearchAndAIConciergeScreen(
    viewModel: StreamViewModel,
    activeProfile: ProfileEntity,
    onMediaPlayTrigger: (MediaItem) -> Unit
) {
    val allMovies by viewModel.allMediaItems.collectAsStateWithLifecycle()
    var searchTab by remember { mutableStateOf("ai") }
    var searchKeyword by remember { mutableStateOf("") }
    var aiQueryText by remember { mutableStateOf("") }

    val aiRecommendation by viewModel.aiRecommendation.collectAsStateWithLifecycle()
    val aiSearching by viewModel.aiSearching.collectAsStateWithLifecycle()

    var selectedDetailBySearch by remember { mutableStateOf<MediaItem?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val aiPromptSuggestions = listOf(
        "Adrenaline space journey with cosmic horror",
        "Cyberpunk espionage and neon street action",
        "Tranquil Japanese arts or culinary passion",
        "Action anime epic battle in feudal era",
        "Spooky vintage midwest high-voltage mystery"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetstreamPureBlack)
            .statusBarsPadding()
    ) {
        // Tab Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = { searchTab = "ai" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (searchTab == "ai") NetstreamRed else NetstreamGrey,
                    contentColor = NetstreamWhite
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Concierge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { searchTab = "search" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (searchTab == "search") NetstreamRed else NetstreamGrey,
                    contentColor = NetstreamWhite
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Instant Search", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Crossfade(targetState = searchTab, label = "SearchCrossfade") { currentTab ->
            if (currentTab == "ai") {
                // Conversational AI panel
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NetstreamGrey),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NetstreamBorderGrey, RoundedCornerShape(12.dp))
                                .padding(top = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI",
                                        tint = NetstreamRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Cinema Concierge",
                                        color = NetstreamWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Input your precise mood, weather outside, or aesthetic preference, and our predictive Gemini model compiles your perfect streaming watchlist instantly.",
                                    color = NetstreamLightGrey,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = aiQueryText,
                                    onValueChange = { aiQueryText = it },
                                    placeholder = { Text("What are you craving tonight?", color = NetstreamLightGrey, fontSize = 14.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = NetstreamWhite,
                                        unfocusedTextColor = NetstreamWhite,
                                        focusedBorderColor = NetstreamRed,
                                        unfocusedBorderColor = NetstreamBorderGrey,
                                        focusedContainerColor = NetstreamPureBlack,
                                        unfocusedContainerColor = NetstreamPureBlack
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_concierge_input"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        viewModel.searchWithGemini(aiQueryText)
                                        keyboardController?.hide()
                                    })
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        viewModel.searchWithGemini(aiQueryText)
                                        keyboardController?.hide()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NetstreamRed),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_generate_button"),
                                    enabled = aiQueryText.trim().isNotEmpty() && !aiSearching
                                ) {
                                    if (aiSearching) {
                                        CircularProgressIndicator(color = NetstreamWhite, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text("Ask Cinema AI", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Prompt Suggestion Chips row
                    item {
                        Column {
                            Text(
                                text = "Aesthetic Suggestions",
                                color = NetstreamWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                aiPromptSuggestions.forEach { suggest ->
                                    Box(
                                        modifier = Modifier
                                            .background(NetstreamGrey, RoundedCornerShape(16.dp))
                                            .border(1.dp, NetstreamBorderGrey, RoundedCornerShape(16.dp))
                                            .clickable { aiQueryText = suggest }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(suggest, color = NetstreamWhite, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Result item representation
                    if (aiSearching) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = NetstreamRed)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Gemini is assembling your custom stream grid...", color = NetstreamLightGrey, fontSize = 14.sp)
                            }
                        }
                    }

                    if (aiRecommendation != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NetstreamGrey.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, NetstreamRed.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(Icons.Default.ChatBubble, contentDescription = null, tint = NetstreamRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("A.I. Analysis", color = NetstreamRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Text(
                                        text = aiRecommendation!!.analysis,
                                        color = NetstreamWhite,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Your Curated Streamlist",
                                color = NetstreamWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        items(aiRecommendation!!.recItems) { recItem ->
                            SearchResultHorizontalCard(
                                item = recItem,
                                onClicked = { selectedDetailBySearch = recItem },
                                onPlay = onMediaPlayTrigger
                            )
                        }
                    }
                }
            } else {
                // Instant Text Search Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("text_search_input"),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NetstreamLightGrey) },
                        placeholder = { Text("Search by title, genre, actor...", color = NetstreamLightGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NetstreamWhite,
                            unfocusedTextColor = NetstreamWhite,
                            focusedBorderColor = NetstreamRed,
                            unfocusedBorderColor = NetstreamBorderGrey,
                            focusedContainerColor = NetstreamGrey,
                            unfocusedContainerColor = NetstreamGrey
                        )
                    )

                    val filteredMovies = remember(searchKeyword, allMovies) {
                        if (searchKeyword.isBlank()) {
                            allMovies
                        } else {
                            allMovies.filter {
                                it.title.contains(searchKeyword, ignoreCase = true) ||
                                        it.genre.contains(searchKeyword, ignoreCase = true) ||
                                        it.description.contains(searchKeyword, ignoreCase = true)
                            }
                        }
                    }

                    if (filteredMovies.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No streamable releases match your search.", color = NetstreamLightGrey, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredMovies) { sItem ->
                                SearchResultHorizontalCard(
                                    item = sItem,
                                    onClicked = { selectedDetailBySearch = sItem },
                                    onPlay = onMediaPlayTrigger
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedDetailBySearch != null) {
        MovieDetailOverlay(
            mediaItem = selectedDetailBySearch!!,
            viewModel = viewModel,
            onDismiss = { selectedDetailBySearch = null },
            onPlay = {
                onMediaPlayTrigger(it)
                selectedDetailBySearch = null
            }
        )
    }
}

@Composable
fun SearchResultHorizontalCard(
    item: MediaItem,
    onClicked: () -> Unit,
    onPlay: (MediaItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp)
            .clickable { onClicked() }
            .border(0.5.dp, NetstreamBorderGrey, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = NetstreamCardDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(130.dp)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    color = NetstreamWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.year}  •  ${item.duration}",
                    color = NetstreamLightGrey,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.genre,
                    color = NetstreamRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Easy Action Play Arrow button directly
            IconButton(
                onClick = { onPlay(item) },
                modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Quick Streaming Play",
                    tint = NetstreamWhite,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, NetstreamWhite, CircleShape)
                        .padding(4.dp)
                )
            }
        }
    }
}

// ==========================================
// 6. OFFLINE DOWNLOADS SCREEN
// ==========================================
@Composable
fun DownloadsScreen(
    viewModel: StreamViewModel,
    onMediaPlayTrigger: (MediaItem) -> Unit
) {
    val downloadItems by viewModel.downloads.collectAsStateWithLifecycle()
    val allMovies by viewModel.allMediaItems.collectAsStateWithLifecycle()
    var selectedDetailsMovieDL by remember { mutableStateOf<MediaItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetstreamPureBlack)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = "Downloads",
                tint = NetstreamRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "My Cache (Downloads)",
                color = NetstreamWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (downloadItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Empty",
                        tint = NetstreamLightGrey,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No offline content cached currently.",
                        color = NetstreamWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Download movies or episodes to view during commutes or airplane mode seamlessly.",
                        color = NetstreamLightGrey,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(downloadItems) { dl ->
                    DownloadCardView(
                        download = dl,
                        onPlay = {
                            // Find element in catalog
                            val catalogMatch = allMovies.find { it.id == dl.mediaId }
                            if (catalogMatch != null) {
                                onMediaPlayTrigger(catalogMatch)
                            }
                        },
                        onDelete = { viewModel.deleteDownload(dl.mediaId) },
                        onDetails = {
                            val catalogMatch = allMovies.find { it.id == dl.mediaId }
                            if (catalogMatch != null) {
                                selectedDetailsMovieDL = catalogMatch
                            }
                        }
                    )
                }
            }
        }
    }

    if (selectedDetailsMovieDL != null) {
        MovieDetailOverlay(
            mediaItem = selectedDetailsMovieDL!!,
            viewModel = viewModel,
            onDismiss = { selectedDetailsMovieDL = null },
            onPlay = {
                onMediaPlayTrigger(it)
                selectedDetailsMovieDL = null
            }
        )
    }
}

@Composable
fun DownloadCardView(
    download: DownloadEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable { onDetails() }
            .border(1.dp, NetstreamBorderGrey, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = NetstreamCardDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = download.imageUrl,
                    contentDescription = download.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (download.isCompleted) {
                    // Small offline play tag
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable { onPlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Cache",
                                tint = NetstreamPureBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = download.title,
                        color = NetstreamWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${String.format("%.1f", download.sizeMb)} MB  •  Offline Ready Mode",
                        color = NetstreamLightGrey,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (!download.isCompleted) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Caching...", color = NetstreamRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${download.progress}%", color = NetstreamWhite, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { download.progress.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = NetstreamRed,
                            trackColor = NetstreamBorderGrey,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMPLETED",
                            color = Color(0xFF46D369),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove cached",
                            tint = NetstreamLightGrey,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onDelete() }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. DEVICES AND WATCHLIST SCREEN
// ==========================================
@Composable
fun DevicesAndWatchlistScreen(
    viewModel: StreamViewModel,
    onMediaPlayTrigger: (MediaItem) -> Unit
) {
    val watchlistItems by viewModel.watchlist.collectAsStateWithLifecycle()
    val allMovies by viewModel.allMediaItems.collectAsStateWithLifecycle()
    val activeDevices by viewModel.activeDevices.collectAsStateWithLifecycle()
    var selectedWatchlistMovie by remember { mutableStateOf<MediaItem?>(null) }
    var context = LocalContext.current

    val toastTrigger = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetstreamPureBlack)
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Multi-Device Streaming Station
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CastConnected,
                contentDescription = null,
                tint = NetstreamRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Seamless Echo (Multi-Device)",
                color = NetstreamWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Initiate seamless streaming transfer. Cast and lock active content to family screens with a single tap.",
            color = NetstreamLightGrey,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Device List Horizontal loop
        Card(
            colors = CardDefaults.cardColors(containerColor = NetstreamGrey),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NetstreamBorderGrey, RoundedCornerShape(12.dp))
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                activeDevices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (device.isCurrent) Icons.Default.CastConnected else Icons.Default.Tv,
                                contentDescription = null,
                                tint = if (device.isCurrent) NetstreamRed else NetstreamWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = device.name,
                                    color = NetstreamWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = device.status + "  |  " + device.quality,
                                    color = NetstreamLightGrey,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (!device.isCurrent) {
                            Button(
                                onClick = {
                                    viewModel.castStreamToDevice(device.name, "Active Stream Transfer")
                                    android.widget.Toast.makeText(context, "Transferred stream seamlessly to ${device.name}!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NetstreamBorderGrey)
                            ) {
                                Text("Cast Here", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NetstreamWhite)
                            }
                        } else {
                            Text(
                                text = "STREAM ACTIVE",
                                color = NetstreamRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }

                    Divider(color = NetstreamBorderGrey, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        // Watchlist Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = null,
                tint = NetstreamRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "My Personal Watchlist (My List)",
                color = NetstreamWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (watchlistItems.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NetstreamGrey.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your watchlist is empty. Go add some high-definition blockbusters!",
                        color = NetstreamLightGrey,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // Watchlisted Media poster elements row-grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                watchlistItems.forEach { wItem ->
                    val matchedItem = allMovies.find { it.id == wItem.mediaId }
                    if (matchedItem != null) {
                        SearchResultHorizontalCard(
                            item = matchedItem,
                            onClicked = { selectedWatchlistMovie = matchedItem },
                            onPlay = onMediaPlayTrigger
                        )
                    }
                }
            }
        }
    }

    if (selectedWatchlistMovie != null) {
        MovieDetailOverlay(
            mediaItem = selectedWatchlistMovie!!,
            viewModel = viewModel,
            onDismiss = { selectedWatchlistMovie = null },
            onPlay = {
                onMediaPlayTrigger(it)
                selectedWatchlistMovie = null
            }
        )
    }
}

// ==========================================
// 8. HIGH DEFINITION VIDEO STREAMING PLAYER MODULE
// ==========================================
@Composable
fun UltraHDMediaPlayer(
    mediaItem: MediaItem,
    viewModel: StreamViewModel,
    onClosePlayer: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgressSeconds by remember { mutableLongStateOf(0L) }
    val totalSeconds = 7200L // Simulate 2-hour movie stream

    val playerScope = rememberCoroutineScope()

    var isCastingActive by remember { mutableStateOf(false) }
    var castDeviceName by remember { mutableStateOf("Local Phone Display") }

    val activeDevices by viewModel.activeDevices.collectAsStateWithLifecycle()

    var speedMultiplier by remember { mutableStateOf(1.0f) }
    var showCastingSelect by remember { mutableStateOf(false) }

    // Read stored playback history of this media item for active selected profile
    LaunchedEffect(Unit) {
        val currentProfile = viewModel.selectedProfile.value
        if (currentProfile != null) {
            val progressHistory = viewModel.getProgressForMedia(mediaItem.id)
            if (progressHistory != null) {
                currentProgressSeconds = progressHistory.progressMs / 1000L
            }
        }
    }

    // Playback loop ticker logic
    LaunchedEffect(isPlaying, speedMultiplier) {
        if (isPlaying) {
            while (currentProgressSeconds < totalSeconds) {
                delay((1000 / speedMultiplier).toLong())
                currentProgressSeconds += 1
                // Auto-save history checkpoints to Room SQLite
                viewModel.savePlaybackProgress(
                    mediaId = mediaItem.id,
                    currentPositionMs = currentProgressSeconds * 1000,
                    totalDurationMs = totalSeconds * 1000
                )
            }
        }
    }

    // Full screen overlay with pure black backdrops
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Keep controls focused */ }
            .testTag("ultra_hd_player")
    ) {
        // Mocking cinematic visual atmosphere (gradient shift)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E0102).copy(alpha = 0.8f),
                                Color.Black
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.width
                        )
                    )
                }
        )

        // Custom animated playback circles inside black visual field
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PlayCircleFilled else Icons.Default.PauseCircleFilled,
                    contentDescription = null,
                    tint = NetstreamRed,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isCastingActive) "CASTING TO $castDeviceName" else "STREAMING IN ULTRA 4K HD",
                    color = NetstreamRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )

                Text(
                    text = mediaItem.title,
                    color = NetstreamWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 6.dp)
                )

                // Subtitle preview simulator
                Text(
                    text = if (isPlaying) "[Music playing softly in background]" else "[Audio Paused]",
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        // Top bar controls (close, casting, speed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClosePlayer) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Exit Player", tint = NetstreamWhite, modifier = Modifier.size(28.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Streaming speed modifier
                IconButton(onClick = {
                    speedMultiplier = when (speedMultiplier) {
                        1.0f -> 1.5f
                        1.5f -> 2.0f
                        else -> 1.0f
                    }
                }) {
                    Box(
                        modifier = Modifier
                            .background(Color.DarkGray.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${speedMultiplier}x", color = NetstreamWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Casting button
                IconButton(onClick = { showCastingSelect = true }) {
                    Icon(
                        imageVector = if (isCastingActive) Icons.Default.CastConnected else Icons.Default.Cast,
                        contentDescription = "Casting",
                        tint = if (isCastingActive) NetstreamRed else NetstreamWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Bottom Bar Controls panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .navigationBarsPadding()
        ) {
            // Seek scrubber line
            val minutesStr = String.format("%02d:%02d", currentProgressSeconds / 60, currentProgressSeconds % 60)
            val totalMinutesStr = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(minutesStr, color = NetstreamLightGrey, fontSize = 12.sp)
                Text(totalMinutesStr, color = NetstreamLightGrey, fontSize = 12.sp)
            }

            Slider(
                value = currentProgressSeconds.toFloat(),
                onValueChange = { newValue ->
                    currentProgressSeconds = newValue.toLong()
                    viewModel.savePlaybackProgress(mediaItem.id, currentProgressSeconds * 1000, totalSeconds * 1000)
                },
                valueRange = 0f..totalSeconds.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = NetstreamRed,
                    activeTrackColor = NetstreamRed,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Direct Player layout buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10 seconds
                IconButton(onClick = {
                    currentProgressSeconds = (currentProgressSeconds - 10).coerceAtLeast(0)
                }) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = NetstreamWhite, modifier = Modifier.size(32.dp))
                }

                // Play/Pause Big Center Clicker
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .size(56.dp)
                        .background(NetstreamRed, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play toggle",
                        tint = NetstreamWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Fast Forward 10 seconds
                IconButton(onClick = {
                    currentProgressSeconds = (currentProgressSeconds + 10).coerceAtMost(totalSeconds)
                }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = NetstreamWhite, modifier = Modifier.size(32.dp))
                }
            }
        }
    }

    if (showCastingSelect) {
        Dialog(onDismissRequest = { showCastingSelect = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NetstreamGrey),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NetstreamBorderGrey, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Transfer Active Stream",
                        color = NetstreamWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    activeDevices.forEach { dev ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.castStreamToDevice(dev.name, mediaItem.title)
                                    castDeviceName = dev.name
                                    isCastingActive = true
                                    showCastingSelect = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = NetstreamWhite, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(dev.name, color = NetstreamWhite, fontSize = 14.sp)
                        }
                        Divider(color = NetstreamBorderGrey)
                    }

                    if (isCastingActive) {
                        Button(
                            onClick = {
                                isCastingActive = false
                                castDeviceName = "Local Phone Display"
                                showCastingSelect = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NetstreamRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text("Disconnect Casting", color = NetstreamWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UploadMovieDialog(
    onDismiss: () -> Unit,
    viewModel: StreamViewModel
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("movie") } // "movie" or "series"
    var genre by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("1h 45m") }
    var year by remember { mutableStateOf("2026") }
    var rating by remember { mutableStateOf("PG-13") }
    var imageUrl by remember { mutableStateOf("") }
    var backdropUrl by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NetstreamGrey),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, NetstreamBorderGrey, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Publish Release",
                        color = NetstreamWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = NetstreamLightGrey)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = NetstreamLightGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NetstreamWhite,
                        unfocusedTextColor = NetstreamWhite,
                        focusedBorderColor = NetstreamRed,
                        unfocusedBorderColor = NetstreamBorderGrey
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Plot & Description", color = NetstreamLightGrey) },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NetstreamWhite,
                        unfocusedTextColor = NetstreamWhite,
                        focusedBorderColor = NetstreamRed,
                        unfocusedBorderColor = NetstreamBorderGrey
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_description_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // TYPE SELECTOR (movie or series)
                Text("Content Type", color = NetstreamWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("movie" to "Movie", "series" to "TV Show").forEach { pair ->
                        val isSelected = mediaType == pair.first
                        Button(
                            onClick = { 
                                mediaType = pair.first
                                if (mediaType == "series" && duration == "1h 45m") {
                                    duration = "1 Season"
                                } else if (mediaType == "movie" && duration.contains("Season")) {
                                    duration = "1h 45m"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NetstreamRed else Color.White.copy(alpha = 0.1f),
                                contentColor = NetstreamWhite
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(pair.second, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Genre and Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Genre", color = NetstreamLightGrey) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NetstreamWhite,
                            unfocusedTextColor = NetstreamWhite,
                            focusedBorderColor = NetstreamRed,
                            unfocusedBorderColor = NetstreamBorderGrey
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration", color = NetstreamLightGrey) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NetstreamWhite,
                            unfocusedTextColor = NetstreamWhite,
                            focusedBorderColor = NetstreamRed,
                            unfocusedBorderColor = NetstreamBorderGrey
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Year and Rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Year", color = NetstreamLightGrey) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NetstreamWhite,
                            unfocusedTextColor = NetstreamWhite,
                            focusedBorderColor = NetstreamRed,
                            unfocusedBorderColor = NetstreamBorderGrey
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = rating,
                        onValueChange = { rating = it },
                        label = { Text("Rating", color = NetstreamLightGrey) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NetstreamWhite,
                            unfocusedTextColor = NetstreamWhite,
                            focusedBorderColor = NetstreamRed,
                            unfocusedBorderColor = NetstreamBorderGrey
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ARTWORK TEMPLATE PRESETS
                Text("Select Artwork Theme Preset", color = NetstreamWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                val presets = listOf(
                    Triple("Cyber City", "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&q=80&w=400", "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&q=80&w=1200"),
                    Triple("Cosmic Space", "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?auto=format&fit=crop&q=80&w=400", "https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?auto=format&fit=crop&q=80&w=1200"),
                    Triple("Gothic Forest", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&q=80&w=400", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&q=80&w=1200"),
                    Triple("Action Anime", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&q=80&w=400", "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&q=80&w=1200")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(presets) { p ->
                        val isSelected = imageUrl == p.second
                        Card(
                            onClick = {
                                imageUrl = p.second
                                backdropUrl = p.third
                                if (genre.isEmpty()) {
                                    genre = if (p.first == "Cyber City") "Cyberpunk Action" else if (p.first == "Cosmic Space") "Sci-Fi Thriller" else if (p.first == "Gothic Forest") "Fantasy Drama" else "Action Anime"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) NetstreamRed else NetstreamBorderGrey),
                            modifier = Modifier
                                .width(90.dp)
                                .height(60.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = p.second,
                                    contentDescription = p.first,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(p.first, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Poster/Backdrop URL toggles (collapsible optional custom fields)
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Poster URL (Optional)", color = NetstreamLightGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NetstreamWhite,
                        unfocusedTextColor = NetstreamWhite,
                        focusedBorderColor = NetstreamRed,
                        unfocusedBorderColor = NetstreamBorderGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = backdropUrl,
                    onValueChange = { backdropUrl = it },
                    label = { Text("Backdrop URL (Optional)", color = NetstreamLightGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NetstreamWhite,
                        unfocusedTextColor = NetstreamWhite,
                        focusedBorderColor = NetstreamRed,
                        unfocusedBorderColor = NetstreamBorderGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Video Streaming URL", color = NetstreamLightGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NetstreamWhite,
                        unfocusedTextColor = NetstreamWhite,
                        focusedBorderColor = NetstreamRed,
                        unfocusedBorderColor = NetstreamBorderGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error message
                var validationError by remember { mutableStateOf<String?>(null) }
                validationError?.let { err ->
                    Text(text = err, color = NetstreamRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                }

                // Publish CTA Button
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            validationError = "Please enter a valid release title."
                            return@Button
                        }
                        if (description.isBlank()) {
                            validationError = "Please write a plot synopsis description."
                            return@Button
                        }
                        if (genre.isBlank()) {
                            validationError = "Please specify a genre style."
                            return@Button
                        }
                        val parsedYear = year.toIntOrNull() ?: 2026

                        viewModel.uploadMovie(
                            title = title,
                            description = description,
                            type = mediaType,
                            genre = genre,
                            duration = duration,
                            rating = rating,
                            year = parsedYear,
                            imageUrl = imageUrl,
                            backdropUrl = backdropUrl,
                            videoUrl = videoUrl
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetstreamRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("publish_movie_button")
                ) {
                    Text("Publish Release", color = NetstreamWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// NETFLIX-STYLE INTRO SPLASH SCREEN
// ==========================================

data class SpectrumLine(
    val xRel: Float, // relative index -0.5 to 0.5 (offset from center)
    val widthDp: androidx.compose.ui.unit.Dp,
    val color: Color,
    val speedMultiplier: Float,
    val heightRatio: Float
)

private val spectrumConfig = listOf(
    SpectrumLine(-0.45f, 2.dp, Color(0xFFE50914), 1.2f, 0.7f),
    SpectrumLine(-0.35f, 3.dp, Color(0xFFF05138), 1.5f, 0.9f),
    SpectrumLine(-0.25f, 1.dp, Color(0xFFFF1E27), 0.9f, 0.5f),
    SpectrumLine(-0.15f, 4.dp, Color(0xFF7E3794), 1.7f, 1.1f),
    SpectrumLine(-0.08f, 2.dp, Color(0xFF3855A5), 1.4f, 0.8f),
    SpectrumLine(0.02f, 5.dp, Color(0xFFE50914), 1.8f, 1.2f),
    SpectrumLine(0.12f, 3.dp, Color(0xFFFCD015), 1.3f, 0.9f),
    SpectrumLine(0.20f, 1.5.dp, Color(0xFF00A2E2), 1.0f, 0.6f),
    SpectrumLine(0.32f, 4.dp, Color(0xFFE50914), 1.6f, 1.0f),
    SpectrumLine(0.42f, 2.5.dp, Color(0xFFF78F26), 1.1f, 0.75f),
    SpectrumLine(-0.40f, 1.5.dp, Color(0xFFE50914), 1.1f, 0.65f),
    SpectrumLine(-0.20f, 2.dp, Color(0xFF7E3794), 1.3f, 0.8f),
    SpectrumLine(-0.10f, 1.dp, Color(0xFFF78F26), 2.0f, 1.0f),
    SpectrumLine(0.05f, 3.dp, Color(0xFFFF1E27), 1.6f, 1.15f),
    SpectrumLine(0.15f, 2.dp, Color(0xFF3855A5), 1.2f, 0.7f),
    SpectrumLine(0.28f, 4.dp, Color(0xFFE50914), 1.8f, 1.1f),
    SpectrumLine(0.38f, 1.5.dp, Color(0xFFFCD015), 1.4f, 0.85f),
    SpectrumLine(0.48f, 3.dp, Color(0xFF7E3794), 1.5f, 0.95f)
)

private fun playTaDum(): AudioTrack? {
    try {
        val sampleRate = 44100
        val durationSeconds = 2.4f
        val numSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            var sample = 0f
            
            // "Ta" impact (starts at 0.1s, duration ~0.15s)
            if (t >= 0.1f && t < 0.28f) {
                val progress = (t - 0.1f) / 0.18f
                val envelope = exp(-progress * 6f)
                val wave = sin(2 * PI * 95 * (t - 0.1f)) + 0.5 * sin(2 * PI * 140 * (t - 0.1f))
                sample += (wave * envelope * 14000f).toFloat()
            }
            
            // "Dum" resonant ring (starts at 0.25s, duration ~2.0s)
            if (t >= 0.25f) {
                // Rapid attack for 30ms, then exponential decay
                val envelope = if (t < 0.28f) {
                    (t - 0.25f) / 0.03f
                } else {
                    exp(-(t - 0.28f) * 1.8f)
                }
                
                // Build rich, movie-theater power chord
                val wave = 0.55f * sin(2 * PI * 58 * (t - 0.25f)) +      // Very deep rumble sub
                           0.45f * sin(2 * PI * 90 * (t - 0.25f)) +      // Hard impact base
                           0.35f * sin(2 * PI * 135 * (t - 0.25f)) +     // Resonant harmonic
                           0.25f * sin(2 * PI * 180 * (t - 0.25f)) +     // Upper body
                           0.15f * sin(2 * PI * 225 * (t - 0.25f))       // High chime ring
                
                sample += (wave * envelope * 16000f).toFloat()
            }
            
            buffer[i] = sample.coerceIn(-32768f, 32767f).toInt().toShort()
        }
        
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        return audioTrack
    } catch (t: Throwable) {
        android.util.Log.e("NetStreamIntro", "Failed to play TaDum sound safely", t)
        return null
    }
}

@Composable
fun NetStreamIntroScreen(onFinished: () -> Unit) {
    val animProgress = remember { Animatable(0f) }
    var audioTrackRef by remember { mutableStateOf<AudioTrack?>(null) }
    
    // Auto start animation and audio
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            try {
                audioTrackRef = playTaDum()
            } catch (t: Throwable) {
                android.util.Log.e("NetStreamIntro", "Sound play exception in launch block", t)
            }
        }
        
        // Progress from 0f to 1f over 3200ms
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3200, easing = LinearEasing)
        )
        onFinished()
    }

    // Direct cleanup of audio if skipped or exited
    DisposableEffect(Unit) {
        onDispose {
            try {
                audioTrackRef?.stop()
                audioTrackRef?.release()
            } catch (ignored: Throwable) {}
        }
    }
    
    val p = animProgress.value
    
    // Scale and opacity calculation
    val scale = remember(p) {
        if (p < 0.35f) {
            // Stage 1: Fast initial snap slide & scale reveal
            val t = p / 0.35f
            val easeOutBack = 1f + 0.3f * (1f - t) * (1f - t) * (1f - t) - 0.3f * (1f - t)
            0.35f + easeOutBack * 0.65f
        } else if (p < 0.70f) {
            // Stage 2: Slow dramatic camera hover zoom
            val t = (p - 0.35f) / 0.35f
            1.0f + t * 0.25f
        } else {
            // Stage 3: Hyperspace dive zooms past camera!
            val t = (p - 0.70f) / 0.30f
            1.25f + (t * t * t) * 26.75f
        }
    }
    
    val alpha = remember(p) {
        if (p < 0.15f) {
            p / 0.15f
        } else if (p < 0.75f) {
            1f
        } else {
            val t = (p - 0.75f) / 0.20f
            (1f - t).coerceIn(0f, 1f)
        }
    }
    
    val ambientGlowAlpha = remember(p) {
        if (p < 0.40f) {
            (p / 0.40f) * 0.5f
        } else if (p < 0.75f) {
            0.5f
        } else {
            val t = (p - 0.75f) / 0.25f
            ((1f - t) * 0.5f).coerceIn(0f, 0.5f)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        // 1. SOFT RED AMBIENT BACKGROUND GLOW
        if (ambientGlowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE50914).copy(alpha = ambientGlowAlpha),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.width * 0.75f
                            ),
                            radius = size.width * 0.75f,
                            center = center
                        )
                    }
            )
        }
        
        // 2. THE GIANT RIBBON "N" SPLASH LOGO
        if (alpha > 0f) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    val rw = w * 0.24f
                    val gap = w * 0.14f
                    
                    val leftStart = w * 0.19f
                    val leftEnd = leftStart + rw
                    val rightStart = leftEnd + gap
                    val rightEnd = rightStart + rw
                    
                    // Dark red gradient for back upright loops
                    val uprightGradient = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE50914), Color(0xFF7A0407)),
                        startY = 0f,
                        endY = h
                    )
                    
                    // Bright vibrant red gradient for front diagonal fold
                    val diagonalGradient = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF242D), Color(0xFFE50914), Color(0xFF9E0409)),
                        start = Offset(leftStart, 0f),
                        end = Offset(rightEnd, h)
                    )
                    
                    // Left Ribbon
                    val leftPath = Path().apply {
                        moveTo(leftStart, 0f)
                        lineTo(leftEnd, 0f)
                        lineTo(leftEnd, h)
                        lineTo(leftStart, h)
                        close()
                    }
                    drawPath(leftPath, brush = uprightGradient)
                    
                    // Right Ribbon
                    val rightPath = Path().apply {
                        moveTo(rightStart, 0f)
                        lineTo(rightEnd, 0f)
                        lineTo(rightEnd, h)
                        lineTo(rightStart, h)
                        close()
                    }
                    drawPath(rightPath, brush = uprightGradient)
                    
                    // Center Diagonal
                    val diagonalPath = Path().apply {
                        moveTo(leftStart, 0f)
                        lineTo(leftStart + rw * 1.05f, 0f)
                        lineTo(rightEnd, h)
                        lineTo(rightEnd - rw * 1.05f, h)
                        close()
                    }
                    drawPath(diagonalPath, brush = diagonalGradient)
                    
                    // Fold shadow
                    val leftShadow = Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                        startX = leftEnd,
                        endX = leftEnd + rw * 0.5f
                    )
                    drawRect(
                        brush = leftShadow,
                        topLeft = Offset(leftEnd, 0f),
                        size = androidx.compose.ui.geometry.Size(rw * 0.5f, h * 0.35f)
                    )
                }
            }
        }
        
        // 3. WARP TUNNEL SPECTRUM LIGHT LINES
        if (p >= 0.65f) {
            val spectrumAlphaVal = if (p < 0.85f) {
                ((p - 0.65f) / 0.20f).coerceIn(0f, 1f)
            } else {
                ((1f - p) / 0.15f).coerceIn(0f, 1f)
            }
            
            val scaleFactor = (p - 0.65f) / 0.35f
            
            if (spectrumAlphaVal > 0f) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()
                    
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { this.alpha = spectrumAlphaVal }
                    ) {
                        val centerX = widthPx / 2f
                        val centerY = heightPx / 2f
                        
                        spectrumConfig.forEach { line ->
                            val xOffset = line.xRel * widthPx * (1f + scaleFactor * 4.5f)
                            val targetX = centerX + xOffset
                            
                            val currentHeight = heightPx * line.heightRatio * (0.15f + scaleFactor * 5.0f)
                            val topY = centerY - (currentHeight / 2f)
                            
                            val thickness = line.widthDp.toPx() * (1f + scaleFactor * 3.5f)
                            
                            drawRoundRect(
                                color = line.color,
                                topLeft = Offset(targetX - thickness / 2f, topY),
                                size = androidx.compose.ui.geometry.Size(thickness, currentHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(thickness / 2f, thickness / 2f)
                            )
                        }
                    }
                }
            }
        }

        // 4. TAP TO SKIP HINT
        Text(
            text = "Tap to skip",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }
}
