package coverosR3z.system.misc.exceptions

/**
 * This is thrown when someone has "stop()"-ed the
 * queue and it is in the process of waiting for
 * the queue to be empty before fully shutting down, and
 * someone tries to enqueue something.
 */
class AttemptToAddToStoppingQueueException : Throwable()
