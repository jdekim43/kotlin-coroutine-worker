package kr.jadekim.worker.coroutine

interface JobDescription<T : Job> {

    val name: String

    fun serialize(job: T): JobData

    fun deserialize(data: JobData): T
}