package kr.jadekim.worker.coroutine

import com.fasterxml.jackson.databind.node.ObjectNode

data class JobData(
        val name: String,
        val data: ObjectNode
)