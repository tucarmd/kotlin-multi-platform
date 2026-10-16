import XCTest

/// [Phụ lục P2 · XCUITest] The same scenarios as SplitScreenUiTest.kt on Android, on the real iOS app.
///
/// Views are found by the ids in shared/.../ui/UiIds.kt. This target runs in a separate process and does not link
/// the Shared framework, so the ids are typed again here: rename one in UiIds.kt → rename it here too.
/// Each test launches a fresh app process, so (unlike Android) no reset is needed between tests.
///
/// Run: xcodebuild test -project iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17'
final class SplitUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launch()
    }

    private func typeBill(_ text: String) {
        let bill = app.textFields["split_bill_input"]
        bill.tap()
        bill.typeText(text)
    }

    func testBill100kTip10ThreePeoplePays37kEach() {
        typeBill("100000")
        // TRAP: segmented Picker segments have no id; pick the tip by its text.
        app.buttons["10%"].tap()
        app.buttons["split_people_plus"].tap()

        XCTAssertEqual(app.staticTexts["split_people_count"].label, "3")
        XCTAssertEqual(app.staticTexts["split_result_per_person"].label, "37.000 đ")
        XCTAssertEqual(app.staticTexts["split_result_tip"].label, "10.000 đ")
        XCTAssertEqual(app.staticTexts["split_result_total"].label, "110.000 đ")
    }

    func testRoundingOffShowsTheExactShare() {
        typeBill("100000")
        app.buttons["split_people_plus"].tap()
        // TRAP: tapping the Toggle element taps the middle of the whole row (the label). The inner switch is the knob.
        app.switches["split_round_up"].switches.firstMatch.tap()

        XCTAssertEqual(app.staticTexts["split_result_per_person"].label, "36.667 đ")
    }

    func testLettersShowAnErrorAndNoResult() {
        typeBill("12a")

        XCTAssertTrue(app.staticTexts["split_bill_error"].waitForExistence(timeout: 2))
        XCTAssertTrue(app.staticTexts["split_result_message"].exists)
        XCTAssertFalse(app.staticTexts["split_result_per_person"].exists)
    }

    func testMinusIsDisabledAtOnePerson() {
        app.buttons["split_people_minus"].tap() // default is 2 people

        XCTAssertEqual(app.staticTexts["split_people_count"].label, "1")
        XCTAssertFalse(app.buttons["split_people_minus"].isEnabled)
    }

    func testResetClearsTheBill() {
        typeBill("50000")
        XCTAssertTrue(app.staticTexts["split_result_per_person"].waitForExistence(timeout: 2))

        app.buttons["split_reset"].tap()

        XCTAssertFalse(app.staticTexts["split_result_per_person"].exists)
        XCTAssertTrue(app.staticTexts["split_result_message"].exists)
    }
}
