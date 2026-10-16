package dev.tipsplit.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.tipsplit.ui.UiIds
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [Phụ lục P2 · Compose UI test] The real app (MainActivity + Koin + TipCore) on an emulator, driven by testTag.
 *
 * Unit tests already prove the maths. These prove the WIRING: the field reaches the core, the buttons are enabled
 * when they should be, and the screen shows the core's answer. Every node is found by an id from the shared
 * UiIds, never by text, so the tests pass in English and Vietnamese.
 *
 * Run: ./gradlew :androidApp:connectedDebugAndroidTest   (an emulator or phone must be connected)
 */
@RunWith(AndroidJUnit4::class)
class SplitScreenUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)

    // TRAP: TipCore is a Koin single that lives as long as the process, and all tests share one process.
    // Without a reset, the bill typed by the previous test is still there.
    @Before
    fun startFromDefaults() {
        node(UiIds.RESET).performClick()
    }

    @Test
    fun bill_100k_tip_10_three_people_pays_37k_each() {
        node(UiIds.BILL_INPUT).performTextInput("100000")
        node(UiIds.tip(10)).performClick()
        node(UiIds.PEOPLE_PLUS).performClick()

        node(UiIds.PEOPLE_COUNT).assertTextEquals("3")
        node(UiIds.RESULT_PER_PERSON).assertTextEquals("37.000 đ")
        node(UiIds.RESULT_TIP).assertTextEquals("10.000 đ")
        node(UiIds.RESULT_TOTAL).assertTextEquals("110.000 đ")
    }

    @Test
    fun rounding_off_shows_the_exact_share() {
        node(UiIds.BILL_INPUT).performTextInput("100000")
        node(UiIds.PEOPLE_PLUS).performClick()
        node(UiIds.ROUND_UP).performClick()

        node(UiIds.ROUND_UP).assertIsOff()
        node(UiIds.RESULT_PER_PERSON).assertTextEquals("36.667 đ")
    }

    @Test
    fun letters_show_an_error_and_no_result() {
        node(UiIds.BILL_INPUT).performTextInput("12a")

        node(UiIds.BILL_ERROR).assertIsDisplayed()
        node(UiIds.RESULT_MESSAGE).assertIsDisplayed()
        node(UiIds.RESULT_PER_PERSON).assertDoesNotExist()
    }

    @Test
    fun minus_is_disabled_at_one_person() {
        node(UiIds.PEOPLE_MINUS).performClick() // default is 2 people

        node(UiIds.PEOPLE_COUNT).assertTextEquals("1")
        node(UiIds.PEOPLE_MINUS).assertIsNotEnabled()
    }

    @Test
    fun reset_clears_the_bill() {
        node(UiIds.BILL_INPUT).performTextInput("50000")
        node(UiIds.RESULT_PER_PERSON).assertIsDisplayed()

        node(UiIds.RESET).performClick()

        node(UiIds.RESULT_PER_PERSON).assertDoesNotExist()
        node(UiIds.RESULT_MESSAGE).assertIsDisplayed()
    }
}
