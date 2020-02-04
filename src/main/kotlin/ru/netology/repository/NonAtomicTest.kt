package ru.netology.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun main() {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var id = 0

    repeat(10_000) {
        scope.launch {
            id++
        }
    }

    Thread.sleep(1000)

    print(id)
}