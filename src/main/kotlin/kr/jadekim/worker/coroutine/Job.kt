package kr.jadekim.worker.coroutine

import kr.jadekim.redis.lettuce.StandaloneRedis
import kr.jadekim.redis.lettuce.StringRedis
import kr.jadekim.worker.coroutine.queue.RedisJobQueue
import kotlin.reflect.KClass

interface Job {

    val description: JobDescription<out Job, out Any>

    suspend fun run()
}

abstract class JobDescription<J : Job, Data : Any>(val jobClass: KClass<J>, val dataClass: KClass<Data>) {

    open val name: String = this::class.java.simpleName

    abstract fun serialize(job: J): Data

    abstract fun deserialize(data: Data): J
}

open class SimpleJobDescription<J : Job>(jobClass: KClass<J>) : JobDescription<J, J>(jobClass, jobClass) {

    override fun serialize(job: J): J = job

    override fun deserialize(data: J): J = data
}

class TestJob : Job {

    companion object Description : SimpleJobDescription<TestJob>(TestJob::class)

    override val description: JobDescription<out Job, out Any> = Description

    override suspend fun run() {
        CoroutineWorker("", RedisJobQueue(StandaloneRedis.stringCodec(""))).apply {
            registerJob(TestJob)
        }
    }

}