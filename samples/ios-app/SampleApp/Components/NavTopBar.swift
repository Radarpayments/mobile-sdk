import SwiftUI

/// Consistent top bar: back chevron (when onBack provided) + title + optional subtitle.
/// Sits at the top of a ScrollView/VStack and inherits the ambient safe area.
struct NavTopBar: View {
    let title: String
    let onBack: (() -> Void)?
    var subtitle: String = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            if let onBack = onBack {
                Button(action: onBack) {
                    HStack(spacing: 2) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(BrandColors.indigo)
                        Text("Back")
                            .font(.system(size: 14))
                            .foregroundColor(BrandColors.indigo)
                    }
                    .padding(.vertical, 4)
                }
                .buttonStyle(.plain)
            }
            Text(title)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color(white: 0.1))
            if !subtitle.isEmpty {
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundColor(Color(white: 0.42))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 10)
        .background(Color.white)
    }
}
