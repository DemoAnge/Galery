package com.example.galeria.features.editor.domain

class EditImageUseCase(
    private val imageEditor: ImageEditor,
) {
    operator fun invoke(input: ByteArray, edit: ImageEdit, maxEdge: Int = 0): ByteArray =
        imageEditor.apply(input, edit, maxEdge)
}
