package kr.jadekim.worker.coroutine.queue

import kr.jadekim.worker.coroutine.JobData

interface JobQueue {

    suspend fun push(jobData: JobData)

    suspend fun pop(): JobData

    suspend fun poll(): JobData?
}