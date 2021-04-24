package coverosR3z.server.utility

import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.RolesChecker
import coverosR3z.system.misc.utility.checkHasRequiredInputs
import coverosR3z.server.api.HomepageAPI
import coverosR3z.server.api.handleUnauthenticated
import coverosR3z.server.api.handleUnauthorized
import coverosR3z.server.types.*
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
        fun doGETRequireAuth(user: User, vararg roles: Role, generator: () -> String): PreparedResponseData {
            return try {
                when (isAuthenticated(user)) {
                    AuthStatus.AUTHENTICATED -> {
                        RolesChecker(CurrentUser(user)).checkAllowed(*roles)
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
         */
        fun doPOSTAuthenticated(
            user: User,
            requiredInputs: Set<Element>,
            data: PostBodyData,
            vararg roles: Role,
            handler: () -> PreparedResponseData
        ): PreparedResponseData {
            return try {
                when (isAuthenticated(user)) {
                    AuthStatus.AUTHENTICATED -> {
                        RolesChecker(CurrentUser(user)).checkAllowed(*roles)
                        checkHasRequiredInputs(data.mapping.keys, requiredInputs)
                        handler()
                    }
                    AuthStatus.UNAUTHENTICATED -> handleUnauthenticated()
                }
            } catch (ex: UnpermittedOperationException) {
                handleUnauthorized(ex.message)
            }
        }

        /**
         * Handle the (rare) situation where a user POSTS data to us
         * and they *must NOT be* authenticated.  Like on the register user page.
         * @param handler the method run to handle the POST
         */
        fun doPOSTRequireUnauthenticated(
            user: User,
            requiredInputs: Set<Element>,
            data: PostBodyData,
            handler: () -> PreparedResponseData
        ): PreparedResponseData {
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
