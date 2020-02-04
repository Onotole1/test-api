package ru.netology.repository

import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.netology.model.PostModel
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

class PostRepositoryMutex : PostRepository {

    private var nextId = 1L
    private val items = mutableListOf<PostModel>()
    private val mutex = Mutex()

    override suspend fun getAll(): List<PostModel> =
        mutex.withLock {
            items.reversed()
        }

    override suspend fun getById(id: Long): PostModel? =
        mutex.withLock {
            items.find { it.id == id }
        }

    override suspend fun save(item: PostModel): PostModel =
        mutex.withLock {
            when (val index = items.indexOfFirst { it.id == item.id }) {
                -1 -> {
                    val copy = item.copy(id = nextId++)
                    items.add(copy)
                    copy
                }
                else -> {
                    items[index] = item
                    item
                }
            }
        }

    override suspend fun removeById(id: Long) {
        mutex.withLock {
            items.removeIf { it.id == id }
        }
    }

    override suspend fun likeById(id: Long): PostModel? =
        mutex.withLock {
            val index = items.indexOfFirst { it.id == id }
            if (index < 0) {
                return@withLock null
            }

            val post = items[index]

            val newPost = post.copy(likes = post.likes.inc())

            items[index] = newPost

            newPost
        }

    override suspend fun dislikeById(id: Long): PostModel? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun main() {
    repeat(10) {
        val repo = PostRepositoryMutex()

        val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

        repeat(10_000) {
            scope.launch {
                repo.save(PostModel(id = 0, author = "Test"))
            }
        }

        Thread.sleep(1000)

        with(CoroutineScope(EmptyCoroutineContext + SupervisorJob())) {
            repeat(100_000) {
                launch {
                    repo.likeById(1L)
                }
            }
        }

        with(CoroutineScope(EmptyCoroutineContext + SupervisorJob())) {
            launch {
                println(repo.getById(1L))
                repo.removeById(1L)
                println("After remove ${repo.getById(1L)}")
            }
        }

        Thread.sleep(2500)

        runBlocking {
            val all = repo.getAll()
            println(all.size)
            File("result.json").writeText(Gson().toJson(all))
        }
    }
}