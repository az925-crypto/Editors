package com.zaaam.editors.core.editor

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.Keep
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CoroutineDispatcher

@Keep
class EditorEngine @JvmOverloads constructor(
    context: Context,
    monoTypeface: Typeface? = null,
    ioDispatcher: CoroutineDispatcher
) : CodeEditor(context) {
    init {
        setTextIsSelectable(false)
        monoTypeface?.also { setTypeface(it) }
    }

    companion object {
        @JvmStatic
        fun create(context: Context, monoTypeface: Typeface? = null, ioDispatcher: CoroutineDispatcher): EditorEngine {
            return EditorEngine(context, monoTypeface, ioDispatcher)
        }
    }
}

class EditorEngineFactory(
    private val ioDispatcher: CoroutineDispatcher
) {
    fun create(context: Context, monoTypeface: Typeface? = null): EditorEngine =
        EditorEngine.create(context, monoTypeface, ioDispatcher)
}