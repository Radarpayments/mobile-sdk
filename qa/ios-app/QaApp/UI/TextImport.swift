import Combine

/// The text typed or pasted on the Import from text screen, read as clipboard text is: on a cloud device the clipboard
/// is not the tester's, but keyboard input is. Held in memory only: a profile's text carries its password.
@MainActor
final class TextImport: ObservableObject {
    @Published private(set) var text = ""
    /// Why the last import was rejected, until the text changes.
    @Published private(set) var problem: Problem?

    /// Off while the text is empty after the trimming of every way in, which would only be the `$` problem.
    var canImport: Bool { !trimInput(text).isEmpty }

    func edit(_ value: String) {
        text = value
        problem = nil
    }

    func clear() {
        edit("")
    }

    /// Hands the text's intake to `accept`, as the clipboard import does. True when the screen is done; on a rejection
    /// the text stays for a fix and `problem` says what is wrong.
    func submit(_ accept: ([Intake]) -> Void) -> Bool {
        guard canImport else { return false }
        let steps = intake(fromText: text)
        accept(steps)
        problem = steps.lazy.compactMap { step -> Problem? in
            if case let .rejected(problem) = step { return problem }
            return nil
        }.first
        if problem == nil { text = "" }
        return problem == nil
    }
}
