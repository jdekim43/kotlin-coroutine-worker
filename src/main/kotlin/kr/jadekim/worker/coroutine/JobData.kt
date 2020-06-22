package kr.jadekim.worker.coroutine

data class JobData(
        val name: String,
        val data: Map<String, String>
)