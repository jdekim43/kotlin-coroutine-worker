package kr.jadekim.worker.coroutine.queue

import kr.jadekim.worker.coroutine.Job

interface JobQueue {

    suspend fun push(job: Job)

    suspend fun pop(): Job

    suspend fun poll(): Job?
}