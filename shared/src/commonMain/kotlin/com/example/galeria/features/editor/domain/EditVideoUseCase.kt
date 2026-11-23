package com.example.galeria.features.editor.domain

class EditVideoUseCase(
    private val videoEditor: VideoEditor,
) {
    operator fun invoke(sourceUri: String, edit: VideoEdit, durationMs: Long): String =
        videoEditor.apply(sourceUri, edit, durationMs)
}
