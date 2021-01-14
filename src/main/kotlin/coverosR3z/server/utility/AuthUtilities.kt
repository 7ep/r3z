package coverosR3z.server.utility

import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.User
import coverosR3z.misc.utility.checkHasExactInputs
import coverosR3z.server.api.HomepageAPI
import coverosR3z.server.api.handleUnauthorized
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.types.Element
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo

/*
These are utilities to standardize handling of various scenarious
for our endpoints.  For example, if a user come in authenticated to
the homepage path, they will be sent to a different homepage than
if they come in unauthenticated.
 */
class AuthUtilities {

    companion object {
        /**
         * If we are authenticated, runs some calculations.  Otherwise
         * redirects to the homepage.
         * @param generator the code to run to generate a string to return for this GET
         */
        fun doGETRequireAuth(authStatus: AuthStatus, generator: () -> String): PreparedResponseData {
            return when (authStatus) {
                AuthStatus.AUTHENTICATED -> okHTML(generator())
                AuthStatus.UNAUTHENTICATED -> redirectTo(HomepageAPI.path)
            }
        }

        /**
         * This is the method for when we want to go either one direction
         * if authenticated or another if unauthenticated.  Most likely
         * example: the homepage
         */
        fun doGETAuthAndUnauth(
            authStatus: AuthStatus,
            generatorAuthenticated: () -> String,
            generatorUnauth: () -> String
        ): PreparedResponseData {
            return when (authStatus) {
                AuthStatus.AUTHENTICATED -> okHTML(generatorAuthenticated())
                AuthStatus.UNAUTHENTICATED -> okHTML(generatorUnauth())
            }
        }

        /**
         * This is for those odd cases where you aren't allowed to go
         * there if you *are* authenticated, like the login page or
         * register user page
         */
        fun doGETRequireUnauthenticated(authStatus: AuthStatus, generator: () -> String): PreparedResponseData {
            return when (authStatus) {
                AuthStatus.UNAUTHENTICATED -> okHTML(generator())
                AuthStatus.AUTHENTICATED -> redirectTo(HomepageAPI.path)
            }
        }

        /**
         * Handle the (pretty ordinary) situation where a user POSTS data to us
         * and they have to be authenticated to do so
         * @param handler the method run to handle the POST
         */
        fun doPOSTAuthenticated(
            authStatus: AuthStatus,
            requiredInputs: Set<Element>,
            data: PostBodyData,
            handler: () -> PreparedResponseData
        ): PreparedResponseData {
            return when (authStatus) {
                AuthStatus.AUTHENTICATED -> {
                    checkHasExactInputs(data.mapping.keys, requiredInputs)
                    handler()
                }
                AuthStatus.UNAUTHENTICATED -> handleUnauthorized()
            }
        }

        /**
         * Handle the (rare) situation where a user POSTS data to us
         * and they *must NOT be* authenticated.  Like on the register user page.
         * @param handler the method run to handle the POST
         */
        fun doPOSTRequireUnauthenticated(
            authStatus: AuthStatus,
            requiredInputs: Set<Element>,
            data: PostBodyData,
            handler: () -> PreparedResponseData
        ): PreparedResponseData {
            return when (authStatus) {
                AuthStatus.UNAUTHENTICATED -> {
                    checkHasExactInputs(data.mapping.keys, requiredInputs)
                    handler()
                }
                AuthStatus.AUTHENTICATED -> redirectTo(HomepageAPI.path)
            }
        }


        fun isAuthenticated(u: User): AuthStatus {
            return if (u != NO_USER) {
                AuthStatus.AUTHENTICATED
            } else {
                AuthStatus.UNAUTHENTICATED
            }
        }
    }
}
