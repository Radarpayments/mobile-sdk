import RadarpaymentsSDKCore
import XCTest

final class SPMCoreCheckTests: XCTestCase {

    func testVersion() {
        XCTAssertFalse(SdkCore().version().isEmpty)
        if let expected = ProcessInfo.processInfo.environment["EXPECTED_SDK_VERSION"] {
            XCTAssertEqual(SdkCore().version(), expected)
        }
    }
}
