package com.zaaam.editors
import android.app.Application
import com.zaaam.editors.di.AppContainer
class EditorsApp : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        instance = this
    }
    companion object {
        lateinit var instance: EditorsApp
            private set
    }
}
