package kr.jadekim.worker.coroutine

data class JobDescription(
        val name: String,
        val step: List<JobStep>
)

@Suppress("FunctionName")
fun Job(name: String, body: (JobParameter) -> Unit) = JobDescription(name, listOf(SingleJobStep(body)))

private class SingleJobStep(private val body: (JobParameter) -> Unit) : JobStep {

    override suspend fun run(parameter: JobParameter, previousResult: JobParameter): JobParameter {
        body(parameter)

        return JobParameter()
    }
}