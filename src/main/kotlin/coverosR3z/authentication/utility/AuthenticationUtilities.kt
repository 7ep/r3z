package coverosR3z.authentication.utility

import coverosR3z.authentication.types.*
import coverosR3z.persistence.types.DataAccess
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.system.logging.ILogger
import coverosR3z.system.misc.types.DateTime
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.NO_EMPLOYEE

class AuthenticationUtilities(
    val pmd: PureMemoryDatabase,
    val logger: ILogger,
    val cu: CurrentUser,
    private val rc: IRolesChecker = RolesChecker
) : IAuthenticationUtilities {

    private val userDataAccess: DataAccess<User> = pmd.dataAccess(User.directoryName)
    private val sessionDataAccess: DataAccess<Session> = pmd.dataAccess(Session.directoryName)
    private val invitationDataAccess: DataAccess<Invitation> = pmd.dataAccess(Invitation.directoryName)

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
        val isUserRegistered = userDataAccess.read { users -> users.singleOrNull { u -> u.name == username } ?: NO_USER } != NO_USER
        return if (! isUserRegistered) {
            //Registration success -> add the user to the database
            val salt = Hash.getSalt()
            val newUser = userDataAccess.actOn { users ->
                logger.logTrace { "PMD: adding new user, \"${username.value}\"" }
                val newUser = User(UserId(users.nextIndex.getAndIncrement()), username, Hash.createHash(password, salt), salt, employee,  Role.REGULAR)
                users.add(newUser)
                newUser
            }
            logger.logDebug { "User registration successful for \"${username.value}\"" }
            RegistrationResult(RegistrationResultStatus.SUCCESS, newUser)
        } else {
            logger.logDebug { "User ${username.value} could not be registered: already registered" }
            RegistrationResult(RegistrationResultStatus.USERNAME_ALREADY_REGISTERED, NO_USER)
        }
    }

    fun isUserRegistered(username: UserName) : Boolean {
        return userDataAccess.read { users -> users.singleOrNull { u -> u.name == username } ?: NO_USER } != NO_USER
    }

    /**
     * Takes a user's username and password and returns a result, and a user
     * as well if the [LoginResult] was successful.
     */
    override fun login(username: UserName, password: Password): Pair<LoginResult, User> {
        val user = userDataAccess.read { users ->  users.singleOrNull { u -> u.name == username } ?: NO_USER }

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
        return sessionDataAccess.read { sessions -> sessions.singleOrNull { it.sessionId == sessionToken }?.user ?: NO_USER }
    }

    fun getAllSessions() : List<Session> {
        return sessionDataAccess.read { sessions -> sessions }.toList()
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
        sessionDataAccess.read { sessions -> require(sessions.none { it.sessionId == sessionId }) { "There must not already exist a session for (${user.name}) if we are to create one" } }
        sessionDataAccess.actOn { sessions -> sessions.add(Session(sessions.nextIndex.getAndIncrement(), sessionId, user, time)) }
        return sessionId
    }

    override fun logout(user: User) {
        sessionDataAccess.read { sessions ->  check(sessions.any{it.user == user}) {"There must exist a session in the database for (${user.name.value}) in order to delete it"} }
        sessionDataAccess.actOn { sessions -> sessions.filter { it.user == user }.forEach { sessions.remove(it) } }
    }

    override fun addRoleToUser(user: User, role: Role) : User {
        rc.checkAllowed(cu, Role.ADMIN, Role.SYSTEM)
        val changedUser = user.copy(role=role)
        userDataAccess.actOn { users -> users.update(changedUser) }
        return changedUser
    }

    override fun createInvitation(employee: Employee,
                         datetime : DateTime,
                         randomCode : () -> String) : Invitation {
        return invitationDataAccess.actOn { invitations ->
            val newInvitation = Invitation(InvitationId(invitations.nextIndex.getAndIncrement()), InvitationCode(randomCode()), employee, datetime)
            invitations.add(newInvitation)
            newInvitation
        }
    }

    override fun getEmployeeFromInvitationCode(invitationCode: InvitationCode): Employee {
        return invitationDataAccess.read { invitations ->
            invitations.singleOrNull { it.code == invitationCode }?.employee ?: NO_EMPLOYEE
        }
    }

    override fun removeInvitation(employee: Employee): Boolean {
        val invitation = invitationDataAccess.read { i -> i.single{ it.employee == employee} }
        return invitationDataAccess.actOn { i -> i.remove(invitation) }
    }

    override fun listAllInvitations() : Set<Invitation> {
        return invitationDataAccess.read { it }
    }

    override fun changePassword(user: User, password: Password): ChangePasswordResult {
        val didChangePassword = userDataAccess.actOn { users -> users.update(user.copy(hash = Hash.createHash(password, user.salt))) }
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

        return userDataAccess.read { users -> users.singleOrNull{ it.employee == employee } ?: NO_USER }
    }

    override fun listUsersByRole(role: Role): Set<User> {
        return userDataAccess.read { u -> u.filter { it.role == role }}.toSet()
    }

}