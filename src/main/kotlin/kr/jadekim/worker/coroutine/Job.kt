package kr.jadekim.worker.coroutine

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

abstract class Job<Data : Any, Result>(val description: JobDescription<Job<Data, *>, Data>) {

    abstract suspend fun execute(data: Data): Result

    open fun onSuccess(result: Result) {
        //do nothing
    }

    open fun onError(e: Exception) {
        //do nothing
    }

    open fun isRetry(e: Exception) = true

    @Suppress("UNCHECKED_CAST")
    internal fun toJobData(data: Any): JobData {
        if (!description.dataClass.isInstance(data)) {
            throw IllegalArgumentException()
        }

        return JobData(description.key, description.serialize(data as Data))
    }

    internal suspend fun executeJob(jobData: JobData): Result = try {
        execute(description.deserialize(jobData.data)).also { onSuccess(it) }
    } catch (e: Exception) {
        onError(e)
        throw e
    }
}

var defaultJobJsonMapper = jacksonObjectMapper()

open class JobDescription<J : Job<Data, *>, Data : Any>(val jobClass: KClass<J>, val dataClass: KClass<Data>) {

    open val key: String = jobClass.java.simpleName

    open fun serialize(data: Data): ObjectNode = defaultJobJsonMapper.valueToTree(data)

    open fun deserialize(data: ObjectNode): Data = defaultJobJsonMapper.treeToValue(data, dataClass.java)
}

data class JobData(
        val key: String,
        val data: ObjectNode,
        var retryCount: Int = 0
)