package coverosR3z.authentication.utility

import coverosR3z.authentication.persistence.IAuthPersistence
import coverosR3z.authentication.types.*
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.system.logging.ILogger
import coverosR3z.system.misc.types.DateTime
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.NO_EMPLOYEE

class AuthenticationUtilities(
    private val ap: IAuthPersistence,
    val pmd: PureMemoryDatabase,
    val logger: ILogger,
    val cu: CurrentUser,
    val rc: IRolesChecker = RolesChecker
) : IAuthenticationUtilities {

    /**
     * Register a user through auth persistent, providing a username, password, and employeeId
     */
    override fun register(username: UserName, password: Password, invitationCode: InvitationCode) : RegistrationResult {
        val employee = getEmployeeFromInvitationCode(invitationCode)
        if (employee == NO_EMPLOYEE) {
            return RegistrationResult(RegistrationResultStatus.NO_INVITATION_FOUND, NO_USER)
        }
        return registerWithEmployee(username, password, employee)
    }

    override fun registerWithEmployee(username: UserName, password: Password, employee: Employee): RegistrationResult {
        return if (! ap.isUserRegistered(username)) {
            //Registration success -> add the user to the database
            val salt = Hash.getSalt()
            val newUser = ap.createUser(username, Hash.createHash(password, salt), salt, employee, Role.REGULAR)
            logger.logDebug { "User registration successful for \"${username.value}\"" }
            RegistrationResult(RegistrationResultStatus.SUCCESS, newUser)
        } else {
            logger.logDebug { "User ${username.value} could not be registered: already registered" }
            RegistrationResult(RegistrationResultStatus.USERNAME_ALREADY_REGISTERED, NO_USER)
        }
    }

    fun isUserRegistered(username: UserName) : Boolean {
        return ap.isUserRegistered(username)
    }

    /**
     * Takes a user's username and password and returns a result, and a user
     * as well if the [LoginResult] was successful.
     */
    override fun login(username: UserName, password: Password): Pair<LoginResult, User> {
        val user = ap.getUser(username)

        if (user == NO_USER) {
            logger.logDebug { "Login failed: user ${username.value} is not registered." }
            return Pair(LoginResult.NOT_REGISTERED, NO_USER)
        }

        val hashedSaltedPassword = Hash.createHash(password,user.salt)
        if (user.hash != hashedSaltedPassword) {
            logger.logDebug { "Login failed for user \"${user.name.value}\": Incorrect password." }
            return Pair(LoginResult.FAILURE, NO_USER)
        }

        logger.logDebug { "Login successful for user ${user.name.value}." }
        return Pair(LoginResult.SUCCESS, user)
    }

    override fun getUserForSession(sessionToken: String): User {
        return ap.getUserForSession(sessionToken)
    }

    /**
     * Generates a new session entry for a user, which indicates they are
     * authenticated.  In so doing, we set the date and time when this is
     * happening, and create a secure random session value (a string)
     *
     * @param user the user for whom this session is being created
     * @param time the exact time and date this is being created (optional, has default)
     * @param rand the generator for a random string (optional, has default)
     */
    override fun createNewSession(user: User, time : DateTime, rand : () -> String): String {
        val sessionId = rand()
        logger.logDebug { "New session ID ($sessionId) generated for user (${user.name.value})" }
        ap.addNewSession(sessionId, user, time)
        return sessionId
    }

    override fun logout(user: User) {
        ap.deleteSession(user)
    }

    override fun addRoleToUser(user: User, role: Role) : User {
        rc.checkAllowed(cu, Role.ADMIN, Role.SYSTEM)
        return ap.addRoleToUser(user, role)
    }

    override fun createInvitation(employee: Employee,
                         datetime : DateTime,
                         randomCode : () -> String) : Invitation {
        return ap.createInvitation(employee, datetime, InvitationCode(randomCode()))
    }

    override fun getEmployeeFromInvitationCode(invitationCode: InvitationCode): Employee {
        return ap.getEmployeeFromInvitationCode(invitationCode)
    }

    override fun removeInvitation(employee: Employee): Boolean {
        return ap.removeInvitation(employee)
    }

    override fun listAllInvitations() : Set<Invitation> {
        return ap.listAllInvitations()
    }

    override fun changePassword(user: User, password: Password): ChangePasswordResult {
        val didChangePassword = ap.updateUser(user.copy(hash = Hash.createHash(password, user.salt)))
        return if (didChangePassword) {
            ChangePasswordResult.SUCCESSFULLY_CHANGED
        } else {
            ChangePasswordResult.FAILED_TO_CHANGE
        }
    }

    override fun getUserByEmployee(employee: Employee): User {
        if (employee == NO_EMPLOYEE) {
            return NO_USER
        }

        return ap.getUserByEmployee(employee)
    }

}