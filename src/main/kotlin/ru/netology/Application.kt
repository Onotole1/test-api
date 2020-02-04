package ru.netology

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.NotFoundException
import io.ktor.features.ParameterConversionException
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.cio.EngineMain
import kotlinx.coroutines.runBlocking
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.kodein.di.ktor.KodeinFeature
import org.kodein.di.ktor.kodein
import ru.netology.dto.PostRequestDto
import ru.netology.dto.PostResponseDto
import ru.netology.model.PostModel
import ru.netology.repository.PostRepository
import ru.netology.repository.PostRepositoryMutex

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            serializeNulls()
        }
    }


    install(StatusPages) {
        exception<NotImplementedError> { e ->
            call.respond(HttpStatusCode.NotImplemented, Error("Error"))
            throw e
        }
        exception<ParameterConversionException> { e ->
            call.respond(HttpStatusCode.BadRequest)
            throw e
        }
        exception<Throwable> { e ->
            call.respond(HttpStatusCode.InternalServerError)
            throw e
        }
    }

    install(KodeinFeature) {
        bind<PostRepository>() with singleton {
            PostRepositoryMutex().apply {
                runBlocking {
                    save(PostModel(0, "author", "test"))
                }
            }
        }
    }

    install(Routing) {
        val repo by kodein().instance<PostRepository>()

        route("/api/v1/posts") {
            get {
                val response = repo.getAll().map(PostResponseDto.Companion::fromModel)
                call.respond(response)
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ParameterConversionException("id", "Long")
                val model = repo.getById(id) ?: throw NotFoundException()
                val response = PostResponseDto.fromModel(model)
                call.respond(response)
            }
            post {
                val request = call.receive<PostRequestDto>()
                val model = PostModel(id = request.id, author = request.author, content = request.content)
                val response = repo.save(model)
                call.respond(response)
            }
            delete("/{id}") {
                TODO()
            }
        }
    }
}