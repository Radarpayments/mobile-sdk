import SwiftUI

/// Pins the sample's screens to the light appearance: they are drawn for a light background only.
/// The checkout is presented through UIKit (see `PayrdrCheckout`), so it still sees the device's
/// real appearance, which its `System` theme follows.
struct LightSampleChrome<Content: View>: View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .environment(\.colorScheme, .light)
    }
}
