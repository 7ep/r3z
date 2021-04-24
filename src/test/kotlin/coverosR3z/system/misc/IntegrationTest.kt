package coverosR3z.system.misc

/**
 * Marks a test as integration, which practically speaking
 * means that certain precautions must be taken when running
 * this test in parallel
 * @param usesPort uses a socket port, which means you might want to
 *                 ensure this doesn't use the same port as another test, to
 *                 avoid conflicting. Otherwise you could easily find
 *                 yourself tied to the behavior of a totally unrelated test.
 * @param usesDirectory uses a directory for some purpose.  Again, you
 *                      will want to avoid conflicting with the directory of
 *                      a different test.  Otherwise you could easily find
 *                      yourself tied to the behavior of a totally unrelated test.
 */
annotation class IntegrationTest(val usesPort: Boolean = false, val usesDirectory: Boolean = false)
