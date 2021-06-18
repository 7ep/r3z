package coverosR3z.server.utility

import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.RolesChecker
import coverosR3z.server.api.*
import coverosR3z.system.misc.utility.checkHasRequiredInputs
import coverosR3z.server.types.*
import coverosR3z.server.utility.ServerUtilities.Companion.okHTML
import coverosR3z.server.utility.ServerUtilities.Companion.redirectTo
import coverosR3z.system.misc.exceptions.InexactInputsException

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
        fun doGETRequireAuth(user: User, vararg roles: Role, generator: () -> String): PreparedResponseData {
            return try {
                when (isAuthenticated(user)) {
                    AuthStatus.AUTHENTICATED -> {
                        RolesChecker.checkAllowed(CurrentUser(user), *roles)
                        okHTML(generator())
                    }
                    AuthStatus.UNAUTHENTICATED -> redirectTo(HomepageAPI.path)
                }
            } catch (ex: UnpermittedOperationException) {
                handleUnauthorized(ex.message)
            }
        }

        /**
         * This is for those odd cases where you aren't allowed to go
         * there if you *are* authenticated, like the login page or
         * register user page
         */
        fun doGETRequireUnauthenticated(user: User, generator: () -> String): PreparedResponseData {
            return when (isAuthenticated(user)) {
                AuthStatus.UNAUTHENTICATED -> okHTML(generator())
                AuthStatus.AUTHENTICATED -> redirectTo(HomepageAPI.path)
            }
        }

        /**
         * Handle the (pretty ordinary) situation where a user POSTS data to us
         * and they have to be authenticated to do so
         * @param handler the method run to handle the POST
         * @param messageReturnPage the page we return to if we needed to show the user a message
         */
        fun doPOSTAuthenticated(
            serverData: ServerData,
            requiredInputs: Set<Element>,
            messageReturnPage: String,
            vararg roles: Role,
            handler: () -> PreparedResponseData
        ): PreparedResponseData {
            val user = serverData.ahd.user
            val data = serverData.ahd.data
            val logger = serverData.logger

            return try {
                when (isAuthenticated(user)) {
                    AuthStatus.AUTHENTICATED -> {
                        RolesChecker.checkAllowed(CurrentUser(user), *roles)
                        checkHasRequiredInputs(data.mapping.keys, requiredInputs)
                        handler()
                    }
                    AuthStatus.UNAUTHENTICATED -> handleUnauthenticated()
                }
            } catch (ex: UnpermittedOperationException) {
                handleUnauthorized(ex.message)
            } catch (ex: InexactInputsException) {
                /*
                If we encounter an [InexactInputsException], that means a problem
                with our code, no need to show that in a nice format, throw it
                */
                throw ex
            } catch (ex: Throwable) {
                // If there ane any complaints whatsoever, we return them here
                logger.logTrace { "handling internal server error: ${ex.stackTraceToString()}" }
                MessageAPI.createCustomMessageRedirect(ex.message ?: "", false, messageReturnPage)
            }
        }

        /**
         * Handle the (rare) situation where a user POSTS data to us
         * and they *must NOT be* authenticated.  Like on the register user page.
         * @param handler the method run to handle the POST
         */
        fun doPOSTRequireUnauthenticated(
            serverData: ServerData,
            requiredInputs: Set<Element>,
            handler: () -> PreparedResponseData
        ): PreparedResponseData {
            val user = serverData.ahd.user
            val data = serverData.ahd.data

            return when (isAuthenticated(user)) {
                AuthStatus.UNAUTHENTICATED -> {
                    checkHasRequiredInputs(data.mapping.keys, requiredInputs)
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
