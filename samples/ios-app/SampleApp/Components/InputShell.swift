import SwiftUI

struct InputShell: View {
    let label: String
    @Binding var value: String
    var placeholder: String = ""
    var error: String? = nil
    var singleLine: Bool = true

    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            VStack(alignment: .leading, spacing: 2) {
                Text(label.uppercased())
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundColor(Color(white: 0.54))
                    .tracking(0.4)
                if singleLine {
                    TextField(placeholder, text: $value)
                        .font(.system(size: 14))
                        .foregroundColor(Color(red: 0.102, green: 0.137, blue: 0.255))
                        .focused($focused)
                } else {
                    TextEditor(text: $value)
                        .font(.system(size: 14, design: .monospaced))
                        .foregroundColor(Color(red: 0.102, green: 0.137, blue: 0.255))
                        .frame(minHeight: 80)
                        .focused($focused)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color.white)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(borderColor, lineWidth: focused ? 2 : 1)
            )
            .cornerRadius(10)
            if let error = error {
                Text(error).font(.system(size: 10)).foregroundColor(.red)
            }
        }
    }

    private var borderColor: Color {
        if error != nil { return .red }
        if focused { return BrandColors.indigo }
        return Color(white: 0.88)
    }
}
