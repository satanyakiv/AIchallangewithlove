package com.portfolio.ai_challenge

import android.app.Application
import com.portfolio.ai_challenge.database.di.databaseModule
import com.portfolio.ai_challenge.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AiChallengeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AiChallengeApplication)
            modules(appModule, databaseModule)
        }
    }
}
