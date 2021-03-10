package com.github.oliverjonas.unmeta

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalScope.launch {
            SuspendTest.test()
        }
    }
}

object SuspendTest {
    suspend fun test() {
    }
}