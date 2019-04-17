package id.singd.android.signdsdk.core

open class SingletonHolder<out T, in A, in B>(creator: (A, B) -> T) {
    private var creator: ((A, B) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A, arg2: B): T {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg, arg2)
                instance = created
                creator = null
                created
            }
        }
    }

    fun getInstance() : T? {
        return instance
    }
}