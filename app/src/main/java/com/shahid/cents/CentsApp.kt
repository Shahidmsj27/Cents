package com.shahid.cents

import android.app.Application

class CentsApp : Application() {
    val database by lazy { CentsDatabase.create(this) }
    val repository by lazy { TransactionRepository(database.transactionDao(), database.categoryRuleDao()) }

    override fun onCreate() {
        super.onCreate()
        ThemeManager.load(this)
    }
}
