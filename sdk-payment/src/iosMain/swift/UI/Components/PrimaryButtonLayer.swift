import SwiftUI

/// What the sheet's main button shows.
struct PrimaryButtonFace: Equatable {
    let text: String
    let icon: PaymentIcon?
    let kind: ButtonKind
    let enabled: Bool
    let loading: Bool
}

/// A step's place for the main button, as the step lays it out. `alpha` and `scale` are the step's own while it changes.
struct PrimaryButtonSlot {
    let face: PrimaryButtonFace
    let action: () -> Void
    let anchor: Anchor<CGRect>
    let settled: Bool
    var alpha: Double = 1
    var scale: CGFloat = 1
}

/// What a step shows over the whole sheet, the main button included, laid over `anchor`.
struct SheetPopup {
    let anchor: Anchor<CGRect>
    let content: AnyView
}

struct SheetPopupsKey: PreferenceKey {
    static let defaultValue: [SheetPopup] = []

    static func reduce(value: inout [SheetPopup], nextValue: () -> [SheetPopup]) {
        value.append(contentsOf: nextValue())
    }
}

/// The main buttons' places in the steps on screen, the step drawn last at the end.
struct PrimaryButtonSlotsKey: PreferenceKey {
    static let defaultValue: [PrimaryButtonSlot] = []

    static func reduce(value: inout [PrimaryButtonSlot], nextValue: () -> [PrimaryButtonSlot]) {
        value.append(contentsOf: nextValue())
    }
}

private struct PrimaryButtonLayerKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Whether a main button is drawn by the sheet's `primaryButtonLayer` instead of its step.
    var hasPrimaryButtonLayer: Bool {
        get { self[PrimaryButtonLayerKey.self] }
        set { self[PrimaryButtonLayerKey.self] = newValue }
    }
}

/// Where a main button stands in its step: the button itself is drawn by the sheet's layer.
struct PrimaryButtonPlace: View {
    let face: PrimaryButtonFace
    let action: () -> Void

    @Environment(\.stepPhase) private var phase

    var body: some View {
        Color.clear
            .frame(maxWidth: .infinity)
            .frame(height: checkoutButtonHeight)
            .anchorPreference(key: PrimaryButtonSlotsKey.self, value: .bounds) { anchor in
                [PrimaryButtonSlot(face: face, action: action, anchor: anchor, settled: phase.settled)]
            }
    }
}

extension View {
    /// Stamps the step's own fade and zoom onto the main button's place in it.
    func primaryButtonChange(alpha: Double, scale: CGFloat) -> some View {
        transformPreference(PrimaryButtonSlotsKey.self) { slots in
            for index in slots.indices {
                slots[index].alpha *= alpha
                slots[index].scale *= scale
            }
        }
    }

    /// Draws the steps' main button over them as one button that stays where it is from step to step: while the steps
    /// change it holds the place of the last settled one and only takes the new width, and its label turns over the way
    /// the flow went. A button that comes or goes alone fades and zooms with its step.
    func primaryButtonLayer(forward: Bool) -> some View {
        modifier(PrimaryButtonLayerModifier(forward: forward))
    }
}

private final class HeldPlace {
    var centerX: CGFloat = 0
    var size: CGSize = .zero
    /// From the bottom of the steps to the bottom of the button: the steps' height changes under it.
    var bottomGap: CGFloat = 0
    var known = false
}

private struct PrimaryButtonLayerModifier: ViewModifier {
    let forward: Bool

    @State private var held = HeldPlace()

    func body(content: Content) -> some View {
        content
            .environment(\.hasPrimaryButtonLayer, true)
            .overlayPreferenceValue(PrimaryButtonSlotsKey.self) { slots in
                GeometryReader { proxy in
                    if let newest = slots.last {
                        layer(newest: newest, count: slots.count, proxy: proxy)
                    }
                }
            }
            .overlayPreferenceValue(SheetPopupsKey.self) { popups in
                GeometryReader { proxy in
                    ForEach(popups.indices, id: \.self) { index in
                        let bounds = proxy[popups[index].anchor]
                        popups[index].content
                            .frame(width: bounds.width, height: bounds.height)
                            .position(x: bounds.midX, y: bounds.midY)
                    }
                }
            }
    }

    private func layer(newest: PrimaryButtonSlot, count: Int, proxy: GeometryProxy) -> some View {
        let bounds = proxy[newest.anchor]
        let height = proxy.size.height
        let ownSize = CGSize(width: bounds.width / max(newest.scale, 0.01), height: bounds.height / max(newest.scale, 0.01))
        var centerX = bounds.midX
        var centerY = bounds.midY
        var size = ownSize
        var scale = newest.scale
        var alpha = newest.alpha
        if count > 1 && held.known {
            let progress = CGFloat(newest.alpha)
            centerX = held.centerX + (bounds.midX - held.centerX) * progress
            centerY = height - held.bottomGap - held.size.height / 2
            size = CGSize(
                width: held.size.width + (ownSize.width - held.size.width) * progress,
                height: held.size.height + (ownSize.height - held.size.height) * progress
            )
            scale = 1
            alpha = 1
        } else if newest.settled {
            held.centerX = bounds.midX
            held.size = ownSize
            held.bottomGap = height - bounds.maxY
            held.known = true
        }
        let motion: LabelMotion = count <= 1 ? .inPlace : forward ? .forward : .back
        let face = newest.face
        return Button(action: newest.action) {
            ButtonFace(text: face.text, kind: face.kind, icon: face.icon, loading: face.loading, motion: motion)
        }
        .buttonStyle(PressedButton(enabled: face.enabled))
        .disabled(!face.enabled || face.loading)
        .frame(width: size.width, height: size.height)
        .scaleEffect(scale)
        .opacity(alpha)
        .position(x: centerX, y: centerY)
    }
}
