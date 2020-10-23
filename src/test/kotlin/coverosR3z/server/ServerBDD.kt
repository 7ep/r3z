package coverosR3z.server

import org.junit.Test

/**
 * As a user of r3z
 * I want to access it through a browser
 * So that it is convenient
 */
class ServerBDD {

    /*
     *  Here we are testing that a common browser, Chrome, is usable
     *  for accessing the application.  We will also test on less-common
     *  but text-oriented browsers, like Lynx, in order to make sure we
     *  remain "graceful degradation". This also helps keep up mobile-first,
     *  easily accessible, and highly performant.
     */


    @Test
    fun `happy path - I should be able to see a page - Chrome`() {
        // Given I am a Chrome browser user
        // When I go to the homepage
        // Then I see it successfully in the browser
    }

    /**
     * Lynx is a common text-based browser
     */
    @Test
    fun `happy path - I should be able to see a page - Lynx`() {
        // Given I am a Lynx browser user
        // When I go to the homepage
        // Then I see it successfully in the browser
    }

}