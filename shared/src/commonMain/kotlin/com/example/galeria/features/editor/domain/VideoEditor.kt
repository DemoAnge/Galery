package com.example.galeria.features.editor.domain

expect class VideoEditor() {
    /**
     * Writes an edited copy to a temp file and returns its absolute path.
     * Caller must delete the file after saving it to the gallery.
     */
    fun apply(sourceUri: String, edit: VideoEdit, durationMs: Long): String
}

expect fun deleteEditedVideoFile(path: String)
