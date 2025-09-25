package com.example.photoapp10.feature.search.ui

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.photoapp10.core.di.Modules
import com.example.photoapp10.feature.album.data.AlbumEntity
import com.example.photoapp10.feature.album.domain.AlbumRepository
import com.example.photoapp10.feature.photo.data.PhotoEntity
import com.example.photoapp10.feature.photo.domain.PhotoRepository
import com.example.photoapp10.feature.photo.domain.SortMode
import com.example.photoapp10.feature.search.domain.SearchUseCase
import com.example.photoapp10.feature.settings.data.UserPrefs
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    nav: NavController,
    albumId: Long? = null, // null = global search; id = album-scoped
    initialQuery: String = "" // New parameter for initial search query
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: SearchViewModel = viewModel(
        factory = SearchViewModel.factory(app, albumId, initialQuery)
    )
    val photoResults by vm.photoResults.collectAsState()
    val albumResults by vm.albumResults.collectAsState()
    val sort by vm.sort.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (albumId == null) "Search" else "Search in album") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(painterResource(android.R.drawable.ic_menu_revert), contentDescription = "Back")
                    }
                },
                actions = {
                    if (albumId == null) {
                        // Only show sort menu for photo results
                        DropdownMenuWithSort(current = sort, onChange = vm::setSort)
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {

            // Debounced query field
            TextField(
                value = vm.query.collectAsState().value,
                onValueChange = vm::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                singleLine = true,
                placeholder = { Text(if (albumId == null) "Search albums and photos" else "Search photos in album") }
            )

            val hasResults = photoResults.isNotEmpty() || (albumId == null && albumResults.isNotEmpty())
            if (!hasResults && vm.query.collectAsState().value.isNotBlank()) {
                EmptyHelp()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show albums first (only for global search)
                    if (albumId == null && albumResults.isNotEmpty()) {
                        item { Text("Albums", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                        items(albumResults, key = { "album_${it.id}" }) { album -> 
                            AlbumRow(album) { nav.navigate("album/${album.id}") }
                        }
                        if (photoResults.isNotEmpty()) {
                            item { Text("Photos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                        }
                    }
                    
                    // Show photos
                    items(photoResults, key = { "photo_${it.id}" }) { p -> 
                        SearchRow(p) { nav.navigate("photo/${p.id}/${p.albumId}") } 
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(album: AlbumEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val photoRepo = Modules.providePhotoRepository(app)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Album cover image
        var coverImagePath by remember(album.id) { mutableStateOf<String?>(null) }
        
        LaunchedEffect(album.id, album.coverPhotoId) {
            try {
                if (album.id > 0) {
                    val coverPhotoId = album.coverPhotoId
                    if (coverPhotoId != null && coverPhotoId > 0) {
                        val photo = photoRepo.getPhoto(coverPhotoId)
                        if (photo != null) {
                            val thumbPath = photo.thumbPath
                            val path = photo.path
                            coverImagePath = if (thumbPath.isNullOrBlank()) {
                                if (path.isNullOrBlank()) null else path
                            } else {
                                thumbPath
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // This is expected when the composition is cancelled
                // Don't log this as an error
            } catch (e: Exception) {
                // Handle any other errors gracefully
                coverImagePath = null
            }
        }
        
        val currentCoverPath = coverImagePath
        if (currentCoverPath != null && currentCoverPath.isNotBlank()) {
            AsyncImage(
                model = currentCoverPath,
                contentDescription = "Album cover",
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback to gray placeholder when no cover is available
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_gallery),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Column(Modifier.weight(1f)) {
            Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${album.photoCount} photos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchRow(p: PhotoEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = p.thumbPath.ifBlank { p.path },
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(p.filename, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val captionText = if (p.caption.isBlank()) "—" else p.caption
            Text(captionText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (p.favorite) {
            Icon(painter = painterResource(android.R.drawable.btn_star_big_on), contentDescription = null)
        }
    }
}

@Composable
private fun EmptyHelp() {
    Column(
        Modifier.fillMaxSize(), 
        verticalArrangement = Arrangement.Center, 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No results", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tips: try a different word, check spelling, or search by a tag/emoji.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DropdownMenuWithSort(current: SortMode, onChange: (SortMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(painter = painterResource(android.R.drawable.arrow_down_float), contentDescription = "Sort")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text("Name A–Z") }, onClick = { expanded = false; onChange(SortMode.NAME_ASC) })
        DropdownMenuItem(text = { Text("Name Z–A") }, onClick = { expanded = false; onChange(SortMode.NAME_DESC) })
        DropdownMenuItem(text = { Text("Date newest") }, onClick = { expanded = false; onChange(SortMode.DATE_NEW) })
        DropdownMenuItem(text = { Text("Date oldest") }, onClick = { expanded = false; onChange(SortMode.DATE_OLD) })
        DropdownMenuItem(text = { Text("Favorites first") }, onClick = { expanded = false; onChange(SortMode.FAV_FIRST) })
    }
}

class SearchViewModel(
    app: Application,
    private val albumId: Long?,
    initialQuery: String = ""
) : AndroidViewModel(app) {

    private val photoRepo = Modules.providePhotoRepository(app)
    private val albumRepo = Modules.provideAlbumRepository(app)
    private val useCase = SearchUseCase(photoRepo)
    private val userPrefs = UserPrefs(app)

    private val _query = MutableStateFlow(initialQuery)
    val query: StateFlow<String> = _query

    private val _sort = userPrefs.sortFlow.stateIn(viewModelScope, SharingStarted.Eagerly, SortMode.DATE_NEW)
    val sort: StateFlow<SortMode> = _sort

    val photoResults: StateFlow<List<PhotoEntity>> =
        useCase.execute(_query, _sort, albumId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val albumResults: StateFlow<List<AlbumEntity>> = 
        if (albumId == null) {
            _query
                .map { it.trim() }
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        albumRepo.searchAlbums("%${query}%")
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        } else {
            MutableStateFlow(emptyList<AlbumEntity>()).asStateFlow()
        }

    fun setQuery(s: String) { 
        _query.value = s 
        // Save last search query when it's not blank
        if (s.isNotBlank()) {
            viewModelScope.launch { userPrefs.setLastSearch(s) }
        }
    }

    fun setSort(mode: SortMode) {
        viewModelScope.launch { userPrefs.setSort(mode) }
    }

    companion object {
        fun factory(app: Application, albumId: Long?, initialQuery: String = "") = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(app, albumId, initialQuery) as T
            }
        }
    }
}
