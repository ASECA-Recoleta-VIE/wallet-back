package com.walletapi.unit

import com.walletapi.exceptions.UserException
import com.walletapi.models.PasswordValidation
import com.walletapi.models.User
import com.walletapi.models.validatePassword
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class UserPasswordTest {

    @Test
    fun validPasswordShouldPassAllChecks() {
        val validPassword = "Password1!"
        val result = validatePassword(validPassword)

        assertEquals(PasswordValidation.VALID, result)

        // Test constructor with valid password should not throw
        val user = User("Test User", "test@example.com", validPassword)
        assertNotNull(user)
    }

    @Test
    fun passwordTooShortShouldFailValidation() {
        val shortPassword = "Pw1!"
        val result = validatePassword(shortPassword)

        assertEquals(PasswordValidation.PASSWORD_TOO_SHORT, result)

        // Test constructor with too short password should throw WeakPasswordException
        val exception = assertFailsWith<UserException.WeakPasswordException> {
            User("Test User", "test@example.com", shortPassword)
        }
        assertEquals("Password must be at least 8 characters long", exception.cause?.message)
    }

    @Test
    fun passwordWithoutUppercaseShouldFailValidation() {
        val noUppercasePassword = "password1!"
        val result = validatePassword(noUppercasePassword)

        assertEquals(PasswordValidation.NO_UPPERCASE, result)

        // Test constructor with no uppercase should throw WeakPasswordException
        val exception = assertFailsWith<UserException.WeakPasswordException> {
            User("Test User", "test@example.com", noUppercasePassword)
        }
        assertEquals("Password must contain at least one uppercase letter", exception.cause?.message)
    }

    @Test
    fun passwordWithoutLowercaseShouldFailValidation() {
        val noLowercasePassword = "PASSWORD1!"
        val result = validatePassword(noLowercasePassword)

        assertEquals(PasswordValidation.NO_LOWERCASE, result)

        // Test constructor with no lowercase should throw WeakPasswordException
        val exception = assertFailsWith<UserException.WeakPasswordException> {
            User("Test User", "test@example.com", noLowercasePassword)
        }
        assertEquals("Password must contain at least one lowercase letter", exception.cause?.message)
    }

    @Test
    fun passwordWithoutDigitShouldFailValidation() {
        val noDigitPassword = "Password!"
        val result = validatePassword(noDigitPassword)

        assertEquals(PasswordValidation.NO_NUMBER, result)

        // Test constructor with no digit should throw WeakPasswordException
        val exception = assertFailsWith<UserException.WeakPasswordException> {
            User("Test User", "test@example.com", noDigitPassword)
        }
        assertEquals("Password must contain at least one number", exception.cause?.message)
    }

    @Test
    fun passwordWithoutSpecialCharacterShouldFailValidation() {
        val noSpecialCharPassword = "Password1"
        val result = validatePassword(noSpecialCharPassword)

        assertEquals(PasswordValidation.NO_SPECIAL_CHARACTER, result)

        // Test constructor with no special character should throw WeakPasswordException
        val exception = assertFailsWith<UserException.WeakPasswordException> {
            User("Test User", "test@example.com", noSpecialCharPassword)
        }
        assertEquals("Password must contain at least one special character", exception.cause?.message)
    }

    @Test
    fun borderlineLengthPasswordShouldBeValidated() {
        // Password with exactly 8 characters
        val exactlyMinLength = "Passw0!d"
        assertEquals(PasswordValidation.VALID, validatePassword(exactlyMinLength))

        // Should create user without throwing exception
        val user = User("Test User", "test@example.com", exactlyMinLength)
        assertNotNull(user)

        // Password with 7 characters
        val tooShort = "Passw0!"
        assertEquals(PasswordValidation.PASSWORD_TOO_SHORT, validatePassword(tooShort))

        // Should throw WeakPasswordException
        assertFailsWith<UserException.WeakPasswordException> {
            User("Test User", "test@example.com", tooShort)
        }
    }

    @Test
    fun complexPasswordWithAllRequirementsShouldBeValid() {
        val complexPassword = "P@ssw0rd123!#"
        assertEquals(PasswordValidation.VALID, validatePassword(complexPassword))

        // Should create user without throwing exception
        val user = User("Test User", "test@example.com", complexPassword)
        assertNotNull(user)
    }

    @Test
    fun orderOfValidationChecksShouldBeConsistent() {
        // Missing multiple requirements, should fail with the first error in the validation chain
        val badPassword = "pass"  // Too short, no uppercase, no digit, no special char

        // It should fail first with the "too short" check since that's the first check in validatePassword
        assertEquals(PasswordValidation.PASSWORD_TOO_SHORT, validatePassword(badPassword))

        // Another example with different order of missing requirements
        val anotherBadPassword = "PASSWORD" // Has uppercase but no lowercase, no digit, no special
        assertEquals(PasswordValidation.NO_LOWERCASE, validatePassword(anotherBadPassword))
    }

    @Test
    fun passwordWithSpaceShouldCountAsSpecialCharacter() {
        val passwordWithSpace = "Password1 "
        assertEquals(PasswordValidation.VALID, validatePassword(passwordWithSpace))

        // Should create user without throwing exception
        val user = User("Test User", "test@example.com", passwordWithSpace)
        assertNotNull(user)
    }
}
