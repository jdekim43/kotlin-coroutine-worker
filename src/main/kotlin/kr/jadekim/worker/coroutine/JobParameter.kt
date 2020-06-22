package kr.jadekim.worker.coroutine

class JobParameter(delegate: Map<String, String?> = emptyMap()) : Map<String, String?> by delegate {

    fun getInt(key: String): Int? = get(key)?.toInt()

    fun getLong(key: String): Long? = get(key)?.toLong()

    fun getDouble(key: String): Double? = get(key)?.toDouble()
}

val Map<String, String?>.asJobParameter
    get() = JobParameter(this)