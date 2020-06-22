package kr.jadekim.worker.coroutine

interface JobStep {

    suspend fun run(parameter: JobParameter, previousResult: JobParameter): JobParameter
}

abstract class RunnableJobStep : JobStep {

    abstract suspend fun run()

    override suspend fun run(parameter: JobParameter, previousResult: JobParameter): JobParameter {
        run()

        return JobParameter()
    }
}

@Suppress("FunctionName")
fun JobStep(block: suspend () -> Unit) = object : RunnableJobStep() {

    override suspend fun run() = block()
}