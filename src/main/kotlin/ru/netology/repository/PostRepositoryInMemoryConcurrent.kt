package ru.netology.repository

import com.google.gson.Gson
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import ru.netology.model.PostModel
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.EmptyCoroutineContext

class PostRepositoryInMemoryConcurrent : PostRepository {

    private var nextId = atomic(0L)
    private val items = CopyOnWriteArrayList<PostModel>()
    override suspend fun getAll(): List<PostModel> {
        return items.reversed()
    }
    override suspend fun getById(id: Long): PostModel? {
        return items.find { it.id == id }
    }
    override suspend fun save(item: PostModel): PostModel {
        return when (val index = items.indexOfFirst { it.id == item.id }) {
            -1 -> {
                val copy = item.copy(id = nextId.incrementAndGet())
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
        items.removeIf { it.id == id }
    }

    override suspend fun likeById(id: Long): PostModel? {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) {
            return null
        }

        val post = items[index]

        val newPost =post.copy(likes = post.likes.inc())

        items[index] = newPost

        return newPost
    }

    override suspend fun dislikeById(id: Long): PostModel? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

/*fun main() {
    val repositoryInMemory = PostRepositoryInMemoryConcurrent()

    val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    repeat(10_000) {
        scope.launch {
            repositoryInMemory.save(PostModel(id = 0, author = "Test $it"))
        }
    }

    Thread.sleep(2000)

    runBlocking {
        val all = repositoryInMemory.getAll()
        println(all.size)
        File("result.json").writeText(Gson().toJson(all))
    }
}*/


fun main() {
    repeat(50) {
        val repo = PostRepositoryInMemoryConcurrent()

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        repeat(10_000) {
            scope.launch {
                repo.save(PostModel(id = 0, author = "Test"))
            }
        }

        Thread.sleep(1000)

        with(CoroutineScope(Dispatchers.IO + SupervisorJob())) {
            repeat(100_000) {
                launch {
                    repo.likeById(1L)
                }
            }
        }

        with(CoroutineScope(Dispatchers.IO + SupervisorJob())) {
            launch {
                println(repo.getById(1L))
                repo.removeById(1L)
                println("After remove ${repo.getById(1L)}")
            }
        }

        Thread.sleep(2000)

        runBlocking {
            val all = repo.getAll()
            println(all.size)
            File("result.json").writeText(Gson().toJson(all))
        }
    }
}
