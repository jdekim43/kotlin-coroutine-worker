package kr.jadekim.worker.coroutine

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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

    private var queuePopper: CoroutineJob? = null
    private val processors = mutableListOf<CoroutineJob>()

    private val jobDescriptions = mutableMapOf<String, JobDescription<Job, Any>>()

    fun start() {
        if (jobDescriptions.isEmpty()) {
            logger.warning("Not registered any job description")
        }

        launchQueuePopper()
        repeat(processorCount) {
            launchProcessor(it.toString())
        }
    }

    suspend fun stop() {
        distributor.close()
        processors.joinAll()
    }

    @Suppress("UNCHECKED_CAST")
    fun <J : Job, Data : Any> registerJob(jobDescription: JobDescription<J, Data>) {
        if (jobDescription.name in jobDescriptions.keys) {
            throw IllegalArgumentException("Already registered job name")
        }

        jobDescriptions[jobDescription.name] = jobDescription as JobDescription<Job, Any>
    }

    operator fun <J : Job> plus(jobDescription: JobDescription<J, Any>) = registerJob(jobDescription)

    private val mapper = jacksonObjectMapper()

    fun run(job: Job): CoroutineJob {
        val description = jobDescriptions[job.description.name] ?: throw IllegalStateException("Not registered job")
        val data = mapper.valueToTree<ObjectNode>(description.serialize(job))
        val jobData = JobData(description.name, data)

        return launch { queue.push(jobData) }
    }

    private fun launchProcessor(id: String? = null) {
        processors += launch(CoroutineName("CoroutineWorker-$name-Processor($id)")) {
            for (jobData in distributor) {
                val description = jobDescriptions[jobData.name]

                if (description == null) {
                    logger.error("Not registered job", extra = mapOf("job" to jobData))

                    continue
                }

                val data = mapper.treeToValue(jobData.data, description.dataClass.java)

                //TODO: Fail over
                description.deserialize(data).run()
            }
        }
    }

    private fun launchQueuePopper() {
        queuePopper = launch(CoroutineName("CoroutineWorker-$name-QueuePopper")) {
            while (isActive) {
                val job = queue.pop()

                if (job == null) {
                    delay(1000)
                    continue
                }

                distributor.send(job) //TODO: POP 된 아이템이 send 되기 전에 취소됐을 경우
            }
        }
    }
}