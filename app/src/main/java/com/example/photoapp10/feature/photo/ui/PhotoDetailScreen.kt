package com.example.photoapp10.feature.photo.ui

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.photoapp10.core.di.Modules
import com.example.photoapp10.feature.photo.data.PhotoEntity
import com.example.photoapp10.feature.photo.domain.PhotoRepository
import com.example.photoapp10.feature.photo.domain.SortMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewModelScope
import java.io.File
import androidx.compose.material3.SnackbarHostState

// Smooth horizontal photo pager using Compose HorizontalPager
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoHorizontalPager(
    photos: List<PhotoEntity>,
    currentPhotoId: Long,
    modifier: Modifier = Modifier,
    onPhotoChanged: (PhotoEntity) -> Unit = {}
) {
    // Ensure photos are consistently ordered (newest first) like Recent/Favourite views
    val sortedPhotos = photos.sortedByDescending { it.createdAt }
    
    val initialIndex = sortedPhotos.indexOfFirst { it.id == currentPhotoId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { sortedPhotos.size }
    )
    
    // Debug logging
    LaunchedEffect(sortedPhotos) {
        android.util.Log.d("PhotoPager", "Sorted photos order: ${sortedPhotos.map { "${it.id}:${it.filename}" }}")
        android.util.Log.d("PhotoPager", "Current photo ID: $currentPhotoId")
        android.util.Log.d("PhotoPager", "Initial index: $initialIndex")
    }
    
    LaunchedEffect(pagerState.currentPage) {
        android.util.Log.d("PhotoPager", "Current page: ${pagerState.currentPage}, Total photos: ${sortedPhotos.size}")
        if (pagerState.currentPage < sortedPhotos.size) {
            val currentPhoto = sortedPhotos[pagerState.currentPage]
            android.util.Log.d("PhotoPager", "Showing photo: ${currentPhoto.filename}")
            onPhotoChanged(currentPhoto)
        }
    }
    
    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->
        if (page < sortedPhotos.size) {
            val photo = sortedPhotos[page]
            AsyncImage(
                model = photo.path,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    photoId: Long,
    albumId: Long,
    nav: NavController
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: PhotoDetailViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(c: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PhotoDetailViewModel(app, photoId, albumId) as T
            }
        }
    )
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val photo by vm.photo.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by vm.snackbarMessage.collectAsState()
    
    // Track the currently viewed photo in the pager
    var currentPhoto by remember { mutableStateOf<PhotoEntity?>(null) }
    
    // Handle snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            vm.clearSnackbarMessage()
        }
    }

    if (photo == null) {
        Text("Photo not found")
        return
    }
    val p = photo!!

    // Initialize currentPhoto with the initial photo
    LaunchedEffect(p.id) {
        currentPhoto = p
    }

    // State for caption editing - reactive to currentPhoto changes
    var isEditingCaption by remember { mutableStateOf(false) }
    var caption by remember(currentPhoto?.id) { 
        mutableStateOf(TextFieldValue(currentPhoto?.caption ?: "")) 
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Photo") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(painterResource(android.R.drawable.ic_menu_revert), contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // Bottom action bar with favorite, share, delete, and edit buttons
            BottomAppBar(
                modifier = Modifier.height(80.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite button
                    IconButton(onClick = { vm.toggleFavorite() }) {
                        Icon(
                            imageVector = if (p.favorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (p.favorite) "Remove from favorites" else "Add to favorites",
                            tint = if (p.favorite) Color(0xFF2196F3) else Color(0xFF666666), // Blue when favorited, gray when not
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Share button
                    IconButton(onClick = {
                        val authority = context.packageName + ".fileprovider"
                        val uri = FileProvider.getUriForFile(context, authority, File(p.path))
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, "Share photo"))
                    }) {
                        Icon(painterResource(android.R.drawable.ic_menu_share), contentDescription = "Share")
                    }
                    
                    // Edit button
                    IconButton(onClick = { isEditingCaption = !isEditingCaption }) {
                        Icon(painterResource(android.R.drawable.ic_menu_edit), contentDescription = "Edit")
                    }
                    
                    // Delete button
                    IconButton(onClick = { vm.requestDelete(onDone = { nav.navigateUp() }) }) {
                        Icon(painterResource(android.R.drawable.ic_menu_delete), contentDescription = "Delete")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // Smooth horizontal photo pager with native-like slide transitions
            val albumPhotos by vm.albumPhotos.collectAsState()
            
            PhotoHorizontalPager(
                photos = albumPhotos,
                currentPhotoId = photoId,
                modifier = Modifier.fillMaxSize(),
                onPhotoChanged = { photo ->
                    currentPhoto = photo
                    // Reset caption editing state when changing photos
                    isEditingCaption = false
                    focusManager.clearFocus()
                }
            )

            // Caption editing overlay
            if (isEditingCaption) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Edit Caption",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        OutlinedTextField(
                            value = caption,
                            onValueChange = { caption = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Caption") },
                            maxLines = 3
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { 
                                    isEditingCaption = false
                                    focusManager.clearFocus()
                                }
                            ) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { 
                                    currentPhoto?.let { photo ->
                                        vm.updateCaption(photo.id, caption.text)
                                        isEditingCaption = false
                                        focusManager.clearFocus()
                                        vm.showSnackbar("Caption saved")
                                    }
                                }
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

class PhotoDetailViewModel(
    app: Application,
    private val photoId: Long,
    private val albumId: Long
) : AndroidViewModel(app) {
    private val photos: PhotoRepository = Modules.providePhotoRepository(app)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    // Get all photos in the album for navigation
    val albumPhotos = photos.observePhotos(albumId, SortMode.DATE_NEW)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Debug: Log when album photos are loaded
        viewModelScope.launch {
            albumPhotos.collect { photos ->
                android.util.Log.d("PhotoDetailVM", "Album photos loaded: ${photos.size} photos for album $albumId")
                photos.forEachIndexed { index, photo ->
                    android.util.Log.d("PhotoDetailVM", "Photo $index: id=${photo.id}, filename=${photo.filename}")
                }
            }
        }
    }

    private val _photo = MutableStateFlow<PhotoEntity?>(null)
    val photo: StateFlow<PhotoEntity?> = _photo

    init {
        viewModelScope.launch {
            _photo.value = photos.getPhoto(photoId)
        }
    }

    fun toggleFavorite() = viewModelScope.launch {
        photos.toggleFavorite(photoId)
        refresh()
    }

    fun updateCaption(photoId: Long, text: String) = viewModelScope.launch {
        photos.updateCaption(photoId, text)
        refresh()
    }

    fun requestDelete(onDone: () -> Unit) = viewModelScope.launch {
        photos.deletePhoto(photoId)
        onDone()
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun navigateToPrevious(): Long? {
        val photos = albumPhotos.value
        val currentIndex = photos.indexOfFirst { it.id == photoId }
        
        return if (currentIndex > 0) {
            photos[currentIndex - 1].id
        } else {
            null
        }
    }

    fun navigateToNext(): Long? {
        val photos = albumPhotos.value
        val currentIndex = photos.indexOfFirst { it.id == photoId }
        
        return if (currentIndex < photos.size - 1) {
            photos[currentIndex + 1].id
        } else {
            null
        }
    }

    private fun refresh() = viewModelScope.launch {
        // Reload the photo after mutation and update the state
        val updated = photos.getPhoto(photoId)
        _photo.value = updated
    }
}

