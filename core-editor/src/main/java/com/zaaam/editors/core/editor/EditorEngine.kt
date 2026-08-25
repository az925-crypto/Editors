package com.zaaam.editors.core.editor

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.EditText
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineDispatcher

@Keep
class EditorEngine @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val monoTypeface: Typeface? = null,
    private val ioDispatcher: CoroutineDispatcher
) : EditText(context, attrs, defStyleAttr) {
    init {
        setTextIsSelectable(false)
        monoTypeface?.let { typeface = it }
    }

    companion object {
        @JvmStatic
        fun create(context: Context, monoTypeface: Typeface? = null, ioDispatcher: CoroutineDispatcher): EditorEngine {
            return EditorEngine(context, null, 0, monoTypeface, ioDispatcher)
        }
    }
}

class EditorEngineFactory(
    private val ioDispatcher: CoroutineDispatcher
) {
    fun create(context: Context, monoTypeface: Typeface? = null): EditorEngine =
        EditorEngine.create(context, monoTypeface, ioDispatcher)
}