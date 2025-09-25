package com.example.photoapp10.feature.search.domain

import com.example.photoapp10.feature.photo.data.PhotoEntity
import com.example.photoapp10.feature.photo.domain.PhotoRepository
import com.example.photoapp10.feature.photo.domain.SortMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class SearchUseCase(private val repo: PhotoRepository) {

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun execute(
        queryFlow: Flow<String>,
        sortFlow: Flow<SortMode>,
        albumId: Long? = null
    ): Flow<List<PhotoEntity>> {
        val q = queryFlow
            .map { it.trim() }
            .debounce(300)
            .distinctUntilChanged()

        return combine(q, sortFlow) { query, sort ->
            query to sort
        }.flatMapLatest { (query, sort) ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repo.search(query, albumId).map { repo.sortList(it, sort) }
            }
        }
    }
}
