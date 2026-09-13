package android.os

open class Handler(looper: Looper? = null) {
    fun post(r: Runnable): Boolean {
        r.run()
        return true
    }
    fun postDelayed(r: Runnable, delayMillis: Long): Boolean {
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(
            r,
            delayMillis,
            java.util.concurrent.TimeUnit.MILLISECONDS
        )
        return true
    }
    fun removeCallbacks(r: Runnable) {}
}
