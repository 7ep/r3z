package coverosR3z.authentication.types

const val passwordMustNotBeNullMsg = "password must not be null"
const val passwordMustNotBeBlankMsg = "password must not be blank"
const val minPasswordSize = 12
const val passwordMustBeLargeEnoughMsg = "password length must be greater than $minPasswordSize"
const val maxPasswordSize = 255
const val passwordMustNotBeTooLargeMsg = "password length must be smaller than $maxPasswordSize"


data class Password(val value: String) {
    init {
        require(value.isNotBlank()) {passwordMustNotBeBlankMsg}
        require(value.length >= minPasswordSize) {passwordMustBeLargeEnoughMsg}
        require(value.length <= maxPasswordSize) {passwordMustNotBeTooLargeMsg}
    }

    companion object {
        fun make(value: String?) : Password {
            val notNullPassword = checkNotNull(value) { passwordMustNotBeNullMsg}
            return Password(notNullPassword)
        }
    }
}