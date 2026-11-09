import SwiftUI

struct ResultHero: View {
    let isSuccess: Bool
    let title: String
    let subtitle: String
    let rows: [(String, String)]

    var body: some View {
        VStack(spacing: 10) {
            ZStack {
                Circle()
                    .fill(isSuccess ? BrandGradient.success.linear()
                                    : LinearGradient(colors: [BrandColors.roseLight, BrandColors.rose],
                                                     startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 56, height: 56)
                Text(isSuccess ? "✓" : "✕").font(.system(size: 28, weight: .bold)).foregroundColor(.white)
            }
            Text(title).font(.system(size: 15, weight: .bold)).foregroundColor(Color(red: 0.102, green: 0.137, blue: 0.255))
            Text(subtitle).font(.system(size: 11)).foregroundColor(Color(white: 0.54))
            VStack(spacing: 0) {
                ForEach(rows.indices, id: \.self) { i in
                    HStack {
                        Text(rows[i].0).font(.system(size: 11)).foregroundColor(Color(white: 0.54))
                        Spacer()
                        Text(rows[i].1).font(.system(size: 11, weight: .semibold)).foregroundColor(Color(red: 0.102, green: 0.137, blue: 0.255))
                    }
                    .padding(.vertical, 6)
                }
            }
            .padding(.top, 6)
        }
        .frame(maxWidth: .infinity)
        .padding(18)
        .background(Color.white)
        .cornerRadius(14)
    }
}
