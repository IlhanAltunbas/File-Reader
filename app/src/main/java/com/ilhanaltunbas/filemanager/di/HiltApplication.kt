package com.ilhanaltunbas.filemanager.di

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HiltApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Uygulama başlatılırken yapılacak işlemleri burada yapabilirsiniz

        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (e:Exception) {
            Log.e("FileManager","PDFBoxResourceLoader başlatılamadı",e)
        }
    }
}