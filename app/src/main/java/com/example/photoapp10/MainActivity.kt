package com.example.photoapp10

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.photoapp10.core.file.FileTestUtils
import com.example.photoapp10.ui.theme.PhotoAppTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Timber FIRST, before any other operations
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.i("MainActivity: onCreate started")
        System.out.println("DEBUG: MainActivity onCreate started")
        
        enableEdgeToEdge()
        Timber.i("MainActivity: enableEdgeToEdge completed")
        System.out.println("DEBUG: enableEdgeToEdge completed")

        // Test file operations for Task 3 acceptance criteria
        Timber.d("Testing AppStorage functionality...")
        try {
            FileTestUtils.runAllTests(this)
        } catch (e: Exception) {
            Timber.e(e, "FileTestUtils failed, but continuing...")
        }
        Timber.i("MainActivity: FileTestUtils completed")
        System.out.println("DEBUG: FileTestUtils completed")

        Timber.i("MainActivity: About to call setContent")
        System.out.println("DEBUG: About to call setContent")
        setContent {
            Timber.i("MainActivity: Inside setContent lambda")
            System.out.println("DEBUG: Inside setContent lambda")
            PhotoAppTheme {
                Timber.i("MainActivity: Inside PhotoAppTheme")
                System.out.println("DEBUG: Inside PhotoAppTheme")
                Surface(modifier = Modifier.fillMaxSize()) {
                    Timber.i("MainActivity: About to call AppNav")
                    System.out.println("DEBUG: About to call AppNav")
                    AppNav()
                    Timber.i("MainActivity: AppNav called successfully")
                    System.out.println("DEBUG: AppNav called successfully")
                }
            }
        }
        Timber.i("MainActivity: setContent completed")
        System.out.println("DEBUG: setContent completed")
    }
}