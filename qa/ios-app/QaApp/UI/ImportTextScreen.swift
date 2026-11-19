import SwiftUI

/// A stand or a configuration typed or pasted into a field, for a device whose clipboard is not the tester's.
struct ImportTextScreen: View {
    @ObservedObject var session: QaSession
    @ObservedObject var textImport: TextImport
    let back: () -> Void

    var body: some View {
        QaScreen(title: "Import from text", back: back, footer: {
            QaFooter {
                FooterWhy(problem: textImport.problem, tag: Tags.validationError)
            } buttons: {
                QaButton(text: "Import", tag: Tags.importTextApply, enabled: textImport.canImport) {
                    if textImport.submit(session.accept) { back() }
                }
            }
        }) {
            Note("A stand or a configuration as JSON or a payrdrqa:// link")
            Caption("Text")
            TextInput(
                text: textImport.text,
                tag: Tags.importTextField,
                isProblem: textImport.problem != nil,
                look: InputLook(singleLine: false, style: QaType.inputSmall)
            ) { textImport.edit($0) }
        }
    }
}
