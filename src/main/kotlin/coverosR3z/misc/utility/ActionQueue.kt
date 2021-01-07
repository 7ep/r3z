package coverosR3z.misc.utility

import coverosR3z.misc.exceptions.AttemptToAddToStoppingQueueException
import coverosR3z.logging.logImperative
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * This provides a way to offload actions, like printing (for logs)
 * or writing files or making directories... anything really, that
 * is a side-effect (that is, it doesn't return a value, it just
 * changes state somewhere).
 *
 * @param name The name of the user of this queue, to aid debugging, for
 *             example: loggingQueue
 */
class ActionQueue(val name : String) {
    private val queue = LinkedBlockingQueue<() -> Unit>()
    private var stop = false

    private val mainThread = thread {
        try {
            while (true) {
                val action = queue.take()
                action.invoke()
            }
        } catch (ex: InterruptedException) {
            /*
            this is what we expect to happen.
            once this happens, we just continue on.
            this only gets called when we are trying to shut everything
            down cleanly
             */
            logImperative("ActionQueue for $name is stopped.")
        }
    }

    fun enqueue(action : () -> Unit) {
        if (stop) {
            throw AttemptToAddToStoppingQueueException()
        }
        queue.add(action)
    }

    /**
     * This will prevent any new actions being
     * queued (by causing an exception to be thrown
     * when a call is made to [enqueue] and will
     * wait until the queue is empty, then shutdown
     * the thread
     */
    fun stop() {
        stop = true
        while(queue.size > 0) {
            Thread.sleep(50)
        }
        mainThread.interrupt()
    }
}