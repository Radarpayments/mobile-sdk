import SwiftUI
import UIKit

extension View {
    /// Tells VoiceOver that a new step is on screen once it has come in, so that it reads the step from its top
    /// instead of staying on a button that is gone.
    func announcesScreenChange(reduceMotion: Bool) -> some View {
        onAppear {
            let settled = reduceMotion ? SheetMotion.reducedEnterDuration : SheetMotion.stepDuration
            DispatchQueue.main.asyncAfter(deadline: .now() + settled) {
                UIAccessibility.post(notification: .screenChanged, argument: nil)
            }
        }
    }

    /// The same for a step with a header, once its change is over, so that VoiceOver reads its title first.
    func announcesScreenChangeWhenSettled() -> some View {
        modifier(SettledScreenChange())
    }
}

private struct SettledScreenChange: ViewModifier {
    @Environment(\.stepPhase) private var phase
    @State private var announced = false

    func body(content: Content) -> some View {
        content
            .onAppear { announce(settled: phase.settled) }
            .onChange(of: phase.settled) { announce(settled: $0) }
    }

    private func announce(settled: Bool) {
        guard settled, !announced else { return }
        announced = true
        // A frame later, as Compose does: the step's elements are in the tree by then.
        DispatchQueue.main.async {
            UIAccessibility.post(notification: .screenChanged, argument: nil)
        }
    }
}
