package com.coveros.r3z

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 *  As an employee
 *  I want to see the expenses I have previously entered
 *  So that I can confirm my expenses been accounted correctly
 */
class EnteringExpensesBDD {

    @Test
    fun `capability to enter new expenses`() {
        `given I have worked 1 hour on project A on Monday`()
        `when I enter in that time`()
        `then The system indicates it has persisted the new information`()
    }

    var context = ""

    @Before
    fun initialize() {
        context = ""
    }

    private fun `given I have worked 1 hour on project A on Monday`() {
    }

    private fun `when I enter in that time`() {
    }

    private fun `then The system indicates it has persisted the new information`() {
        assertTrue(true)
    }

}