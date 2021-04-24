package coverosR3z.authentication.utility

import coverosR3z.authentication.types.*
import coverosR3z.system.config.LENGTH_OF_BYTES_OF_INVITATION_CODE
import coverosR3z.system.config.LENGTH_OF_BYTES_OF_SESSION_STRING
import coverosR3z.system.misc.utility.generateRandomString
import coverosR3z.system.misc.types.DateTime
import coverosR3z.timerecording.types.Employee
import java.time.LocalDateTime
import java.time.ZoneOffset

interface IAuthenticationUtilities {

    /**
     * Register a user the typical way, using an [InvitationCode]
     */
    fun register(username: UserName, password: Password, invitationCode: InvitationCode) : RegistrationResult

    fun registerWithEmployee(username: UserName, password: Password, employee: Employee) : RegistrationResult

    /**
     * Takes a user's username and password and returns a result, and a user
     * as well if the [LoginResult] was successful.
     */
    fun login(username: UserName, password: Password): Pair<LoginResult, User>

    /**
     * Returns the user if there is a valid session,
     * otherwise returns null
     */
    fun getUserForSession(sessionToken: String): User

    /**
     * Adds a new session to the sessions data structure, with
     * the user and a generated session value
     */
    fun createNewSession(
        user: User,
        time : DateTime = DateTime(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)),
        rand : () -> String = { generateRandomString(LENGTH_OF_BYTES_OF_SESSION_STRING) }) : String

    /**
     * Wipes out the session entry for this user
     */
    fun logout(user: User)

    /**
     * sets the [Role] on a [User]
     */
    fun addRoleToUser(user: User, role: Role): User

    /**
     * sets up a code to send to a person so they can
     * register a user that will connect to an employee already
     * created by an admin.
     */
    fun createInvitation(employee: Employee,
                         datetime : DateTime = DateTime(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)),
                         randomCode : () -> String = { generateRandomString(LENGTH_OF_BYTES_OF_INVITATION_CODE) }) : Invitation

    fun getEmployeeFromInvitationCode(invitationCode: InvitationCode): Employee

    /**
     * Removes the invitation for a particular employee.
     * @return true if we removed it, false if there wasn't any invitation to remove
     */
    fun removeInvitation(employee: Employee): Boolean

    fun listAllInvitations() : Set<Invitation>

    fun changePassword(user: User, password: Password): ChangePasswordResult

}
