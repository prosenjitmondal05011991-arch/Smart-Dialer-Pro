package com.example.smartdialer.localization

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.example.smartdialer.data.preferences.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun resolveLocale(language: AppLanguage, systemLocale: Locale = Locale.getDefault()): Locale {
        return when (language) {
            AppLanguage.SYSTEM -> systemLocale
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.BENGALI -> Locale.forLanguageTag("bn")
        }
    }

    fun createLocalizedContext(baseContext: Context, language: AppLanguage): Context {
        val targetLocale = when (language) {
            AppLanguage.SYSTEM -> return baseContext
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.BENGALI -> Locale.forLanguageTag("bn")
        }
        val config = Configuration(baseContext.resources.configuration)
        config.setLocales(LocaleList(targetLocale))
        config.setLocale(targetLocale)
        return baseContext.createConfigurationContext(config)
    }
}
