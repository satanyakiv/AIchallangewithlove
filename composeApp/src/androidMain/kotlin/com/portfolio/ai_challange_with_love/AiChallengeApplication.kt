package com.portfolio.ai_challange_with_love

import android.app.Application
import com.portfolio.ai_challange_with_love.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AiChallengeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AiChallengeApplication)
            modules(appModule)
        }
    }
}
