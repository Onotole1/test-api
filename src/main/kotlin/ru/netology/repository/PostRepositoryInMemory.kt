package ru.netology.repository

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.netology.model.PostModel
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

class PostRepositoryInMemory : PostRepository {

    private var nextId = 1L
    private val items = mutableListOf<PostModel>()
    override suspend fun getAll(): List<PostModel> {
        return items.reversed()
    }
    override suspend fun getById(id: Long): PostModel? {
        return items.find { it.id == id }
    }
    override suspend fun save(item: PostModel): PostModel {
        return when (val index = items.indexOfFirst { it.id == item.id }) {
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

fun main() {
    val repositoryInMemory = PostRepositoryInMemory()

    val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    repeat(10_000) {
        scope.launch {
            repositoryInMemory.save(PostModel(id = it.toLong(), author = "Test"))
        }
    }

    scope.launch {
        File("result.json").writeText(Gson().toJson(repositoryInMemory.getAll()))
    }
}