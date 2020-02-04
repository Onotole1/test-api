package ru.netology.repository

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun main() {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val id = atomic(0)

    repeat(10_000) {
        scope.launch {
            id.incrementAndGet()
        }
    }

    Thread.sleep(1000)

    print(id)
}