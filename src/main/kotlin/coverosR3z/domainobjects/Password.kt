package coverosR3z.domainobjects

private const val passwordMustNotBeNullMsg = "password must not be null"
const val passwordMustNotBeBlankMsg = "password must not be blank"
private const val minPasswordSize = 12
private const val passwordMustBeLargeEnoughMsg = "password length must be greater than $minPasswordSize"
private const val maxPasswordSize = 255
const val passwordMustNotBeTooLargeMsg = "password length must be smaller than $maxPasswordSize"


data class Password(val value: String) {
    init {
        require(value.isNotBlank()) {passwordMustNotBeBlankMsg}
        require(value.length >= minPasswordSize) {passwordMustBeLargeEnoughMsg}
        require(value.length <= maxPasswordSize) {passwordMustNotBeTooLargeMsg}
    }

    fun addSalt(value: Salt) : Password {
        return Password(this.value + value.value)
    }

    companion object {
        fun make(value: String?) : Password {
            val notNullPassword = checkNotNull(value) { passwordMustNotBeNullMsg}
            return Password(notNullPassword)
        }
    }
}