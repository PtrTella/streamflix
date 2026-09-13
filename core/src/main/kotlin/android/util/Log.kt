package android.util

object Log {
    var isDebugEnabled = true

    fun d(tag: String, msg: String): Int {
        if (isDebugEnabled) println("[DEBUG][$tag] $msg")
        return 0
    }

    fun i(tag: String, msg: String): Int {
        println("[INFO][$tag] $msg")
        return 0
    }

    fun w(tag: String, msg: String, tr: Throwable? = null): Int {
        println("[WARN][$tag] $msg")
        tr?.printStackTrace()
        return 0
    }

    fun e(tag: String, msg: String, tr: Throwable? = null): Int {
        System.err.println("[ERROR][$tag] $msg")
        tr?.printStackTrace()
        return 0
    }
}
