package kr.jadekim.worker.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kr.jadekim.logger.JLog
import kr.jadekim.worker.coroutine.queue.JobQueue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Job as CoroutineJob

class CoroutineWorker(
        val name: String,
        val queue: JobQueue,
        val processorCount: Int = Runtime.getRuntime().availableProcessors() * 4,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        context: CoroutineContext = CoroutineName("CoroutineWorker-$name"),
        val pendingSize: Int = Channel.RENDEZVOUS
) : CoroutineScope {

    override val coroutineContext: CoroutineContext = dispatcher + context

    private val logger = JLog.get(javaClass)

    private val distributor = Channel<JobData>(pendingSize)

    private val processors = mutableListOf<CoroutineJob>()

    private var queuePopper: CoroutineJob? = null
    private var isActiveQueuePopper: Boolean = false

    private val jobs = mutableMapOf<String, Job<*, *>>()

    fun start() {
        if (jobs.isEmpty()) {
            logger.warning("Not registered any job")
        }

        launchQueuePopper()
        repeat(processorCount) {
            launchProcessor(it.toString())
        }
    }

    suspend fun stop() {
        isActiveQueuePopper = false
        queuePopper?.join()
        distributor.close()
        processors.joinAll()
    }

    fun registerJob(job: Job<*, *>) {
        if (job.description.key in jobs.keys) {
            throw IllegalArgumentException("Already registered job name")
        }

        jobs[job.description.key] = job
    }

    operator fun plus(job: Job<Any, Any>) = registerJob(job)

    fun <Data : Any> run(jobDescription: JobDescription<Data>, data: Data) = run(jobDescription.key, data)

    fun <Data : Any> run(jobKey: String, data: Data): CoroutineJob {
        val job = jobs[jobKey] ?: throw IllegalStateException("Not registered job")
        val jobData = job.toJobData(data)

        return launch { queue.push(jobData) }
    }

    private fun launchProcessor(id: String? = null) {
        processors += launch(CoroutineName("CoroutineWorker-$name-Processor($id)")) {
            for (jobData in distributor) {
                val job = jobs[jobData.key]

                if (job == null) {
                    logger.error("Not registered job", extra = mapOf("job" to jobData))

                    continue
                }

                try {
                    job.executeJob(jobData)
                } catch (e: Exception) {
                    logger.error("Fail to execute job", e, extra = mapOf("job" to jobData))
                    if (job.isRetry(e)) {
                        jobData.retryCount += 1
                        queue.push(jobData)
                    }
                }
            }
        }
    }

    private fun launchQueuePopper() {
        isActiveQueuePopper = true
        queuePopper = launch(CoroutineName("CoroutineWorker-$name-QueuePopper")) {
            //POP 된 아이템이 send 되기 전에 취소되면서 누락되는 경우를 방지하기 위해
            //CoroutineScope.isActive 대신 다른 변수를 사용하여 coroutineJob 이 정상적으로 종료되도록 함
            while (isActiveQueuePopper) {
                val job = queue.pop()

                if (job == null) {
                    delay(1000)
                    continue
                }

                distributor.send(job)
            }
        }
    }
}