package kr.jadekim.worker.coroutine

data class Job(
        val name: String,
        val parameter: JobParameter
)