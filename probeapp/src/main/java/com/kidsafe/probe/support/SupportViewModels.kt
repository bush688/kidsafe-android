package com.kidsafe.probe.support

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PrivacyUiState(
    val markdown: String = "",
    val consent: PrivacyConsent? = null,
)

class PrivacyViewModel(app: Application) : AndroidViewModel(app) {
    private val assets = AssetTextRepository(app.applicationContext)
    private val prefs = SupportPrefsStore(app.applicationContext)
    private val _state = MutableStateFlow(PrivacyUiState())
    val state: StateFlow<PrivacyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val md = assets.loadLocalizedText("privacy.md").orEmpty()
            _state.value = _state.value.copy(markdown = md)
        }
        viewModelScope.launch {
            prefs.privacyConsent.collect { consent ->
                _state.value = _state.value.copy(consent = consent)
            }
        }
    }

    fun accept(policyId: String) {
        viewModelScope.launch { prefs.acceptPrivacy(policyId) }
    }

    fun withdraw() {
        viewModelScope.launch { prefs.withdrawPrivacyConsent() }
    }
}

data class ReleaseNotesUiState(
    val notes: List<ReleaseNote> = emptyList(),
    val selected: ReleaseNote? = null,
)

class ReleaseNotesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ReleaseNotesRepository(AssetTextRepository(app.applicationContext))
    private val _state = MutableStateFlow(ReleaseNotesUiState())
    val state: StateFlow<ReleaseNotesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val notes = repo.loadNotes()
            _state.value = _state.value.copy(notes = notes)
        }
    }

    fun select(note: ReleaseNote?) {
        _state.value = _state.value.copy(selected = note)
    }
}

data class HelpUiState(
    val categories: List<HelpCategory> = emptyList(),
    val query: String = "",
    val searchResults: List<HelpDocRef> = emptyList(),
    val selectedDoc: HelpDocRef? = null,
    val selectedDocMarkdown: String? = null,
)

class HelpViewModel(app: Application) : AndroidViewModel(app) {
    private val assets = AssetTextRepository(app.applicationContext)
    private val repo = HelpRepository(assets)
    private val _state = MutableStateFlow(HelpUiState())
    val state: StateFlow<HelpUiState> = _state.asStateFlow()

    private var cachedDocs: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch {
            val cats = repo.loadIndex()
            _state.value = _state.value.copy(categories = cats)
            buildCache(cats)
        }
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        val query = q.trim().lowercase()
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList())
            return
        }
        val hits = _state.value.categories
            .flatMap { it.items }
            .distinctBy { it.path }
            .filter { doc ->
                val titleHit = doc.title.lowercase().contains(query)
                val body = cachedDocs[doc.path]?.lowercase().orEmpty()
                titleHit || body.contains(query)
            }
            .take(50)
        _state.value = _state.value.copy(searchResults = hits)
    }

    fun openDoc(doc: HelpDocRef?) {
        _state.value = _state.value.copy(selectedDoc = doc, selectedDocMarkdown = null)
        val selected = doc ?: return
        viewModelScope.launch {
            val md = cachedDocs[selected.path] ?: repo.loadDocMarkdown(selected.path)
            _state.value = _state.value.copy(selectedDocMarkdown = md)
        }
    }

    private fun buildCache(categories: List<HelpCategory>) {
        viewModelScope.launch {
            val all = categories.flatMap { it.items }.distinctBy { it.path }
            val map = withContext(Dispatchers.IO) {
                buildMap {
                    all.forEach { doc ->
                        val md = repo.loadDocMarkdown(doc.path)
                        if (md != null) put(doc.path, md)
                    }
                }
            }
            cachedDocs = map
        }
    }
}

data class UpdateUiState(
    val config: UpdateConfig? = null,
)

class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UpdateRepository(AssetTextRepository(app.applicationContext))
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cfg = repo.loadConfig()
            _state.value = UpdateUiState(cfg)
        }
    }
}

