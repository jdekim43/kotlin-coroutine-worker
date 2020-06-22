package kr.jadekim.worker.coroutine.queue

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.jadekim.redis.lettuce.StringRedis
import kr.jadekim.redis.lettuce.util.RedisQueue
import kr.jadekim.worker.coroutine.Job

class RedisJobQueue(
        private val redis: StringRedis,
        val key: String = "QUEUE:JOB",
        autoStart: Boolean = true
) : JobQueue {

    private val eventBusKey = "$key:EVENT"

    private val delegator = RedisQueue(redis, key, Job::class.java)

    private var eventBus: ReceiveChannel<Pair<String, String>>? = null

    init {
        if (autoStart) {
            GlobalScope.launch { start() }
        }
    }

    suspend fun start() {
        eventBus = redis.subscribe(Channel.CONFLATED).apply {
            subscribe(eventBusKey)
        }.asCoroutineChannel()
    }

    override suspend fun push(job: Job) {
        delegator.push(job)
        redis.execute { publish(eventBusKey, "PUSH") }
    }

    override suspend fun pop(): Job {
        var job: Job? = delegator.pop()

        while (job == null) {
            job = delegator.pop()

            if (eventBus == null) {
                delay(1000)
            } else {
                eventBus!!.receive()
            }
        }

        return job
    }

    override suspend fun poll(): Job? = delegator.pop()
}