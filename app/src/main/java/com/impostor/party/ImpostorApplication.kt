package com.impostor.party

import android.app.Application
import com.impostor.party.data.SettingsRepository
import com.impostor.party.data.WordRepository
import com.impostor.party.util.SoundManager

/**
 * A hand-rolled container instead of a DI framework: three objects, no reflection,
 * and nothing to initialise on the main thread beyond allocating them.
 */
class AppContainer(app: Application) {
    val settings: SettingsRepository = SettingsRepository(app)
    val words: WordRepository = WordRepository(app, settings.prefs)
    val sound: SoundManager = SoundManager(app)
}

class ImpostorApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun onTerminate() {
        container.sound.release()
        super.onTerminate()
    }
}
