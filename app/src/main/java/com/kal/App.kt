package com.kal

import android.app.Application
import com.kal.history.HistoryService

class App: Application() {
    val historyService: HistoryService by lazy {
        HistoryService(this)
    }
}
