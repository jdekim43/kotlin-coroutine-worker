package kr.jadekim.worker.coroutine.queue

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.jadekim.redis.lettuce.StringRedis
import kr.jadekim.redis.lettuce.util.RedisQueue
import kr.jadekim.worker.coroutine.JobData

class RedisJobQueue(
        private val redis: StringRedis,
        val key: String = "QUEUE:JOB",
        autoStart: Boolean = true
) : JobQueue {

    private val eventBusKey = "$key:EVENT"

    private val delegator = RedisQueue(redis, key, JobData::class.java)

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

    override suspend fun push(jobData: JobData) {
        delegator.push(jobData)
        redis.execute { publish(eventBusKey, "PUSH") }
    }

    override suspend fun pop(): JobData {
        var jobData: JobData? = delegator.pop()

        while (jobData == null) {
            jobData = delegator.pop()

            if (eventBus == null) {
                delay(1000)
            } else {
                eventBus!!.receive()
            }
        }

        return jobData
    }

    override suspend fun poll(): JobData? = delegator.pop()
}