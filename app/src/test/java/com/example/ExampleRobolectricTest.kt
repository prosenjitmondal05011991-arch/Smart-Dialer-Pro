package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.smartdialer.data.preferences.AppLanguage
import com.example.smartdialer.localization.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Smart Dialer Pro`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Dialer Pro", appName)
    }

    @Test
    fun `bengali localization provides translated navigation labels`() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val bnContext = LocaleHelper.createLocalizedContext(base, AppLanguage.BENGALI)
        val enContext = LocaleHelper.createLocalizedContext(base, AppLanguage.ENGLISH)

        assertEquals("Favorites", enContext.getString(R.string.nav_favorites))
        assertEquals("প্রিয়", bnContext.getString(R.string.nav_favorites))
        assertFalse(bnContext.getString(R.string.recents_title) == enContext.getString(R.string.recents_title))
    }
}
