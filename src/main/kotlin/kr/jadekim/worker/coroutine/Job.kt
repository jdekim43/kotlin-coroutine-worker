package kr.jadekim.worker.coroutine

interface Job {

    val description: JobDescription<out Job>

    suspend fun run()
}