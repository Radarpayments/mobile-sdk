import SwiftUI

/// Side padding of everything in the checkout.
let sidePadding: CGFloat = 20

/// Between the status bar and the top of a sheet that is not opened to the whole screen.
private let sheetTopGap: CGFloat = 24

/// The dimmed merchant screen under the sheet: fades in with the sheet.
struct Scrim: View {
    let reduceMotion: Bool
    let onTap: () -> Void

    @Environment(\.checkoutTheme) private var theme
    @State private var shown = false

    var body: some View {
        theme.colors.scrim
            .opacity(shown ? 1 : 0)
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)
            .accessibilityHidden(true)
            .onAppear {
                withAnimation(sheetEnterAnimation(reduceMotion)) { shown = true }
            }
    }
}

func sheetEnterAnimation(_ reduceMotion: Bool) -> Animation {
    reduceMotion ? .linear(duration: SheetMotion.reducedSheetEnterDuration) : SheetMotion.sheet(SheetMotion.sheetEnterDuration)
}

func sheetCloseAnimation(_ reduceMotion: Bool) -> Animation {
    reduceMotion ? .linear(duration: SheetMotion.reducedSheetCloseDuration) : SheetMotion.sheet(SheetMotion.sheetCloseDuration)
}

/// The bottom sheet every step lives in: slides up once, follows a downward drag, and opens to the whole screen for
/// the bank's page.
struct SheetHost<Content: View>: View {
    let draggable: Bool
    let showGrabber: Bool
    /// Opened to the whole screen: no gap above, no rounded top, the status bar inside.
    let expanded: Bool
    let reduceMotion: Bool
    let onDismiss: () -> Void
    let onExpanded: () -> Void
    let onSheetHeight: (CGFloat) -> Void
    /// Gets whether the sheet has slid in: native views wait for it.
    @ViewBuilder let content: (_ entered: Bool) -> Content

    @Environment(\.checkoutTheme) private var theme
    @EnvironmentObject private var insets: SheetInsets
    @State private var hidden: CGFloat = 1
    @State private var entered = false
    @State private var open: CGFloat = 0
    @State private var drag: CGFloat = 0
    @State private var height: CGFloat = 0
    @State private var contentScrolls = false
    @State private var enterAnimator = FrameAnimator()

    var body: some View {
        VStack(spacing: 14) {
            if showGrabber {
                SheetGrabber().onTapGesture { if draggable { onDismiss() } }
            }
            content(entered)
        }
        .padding(.top, insets.top * open)
        .padding(.leading, insets.leading)
        .padding(.trailing, insets.trailing)
        .padding(.bottom, insets.sheetBottom)
        .padding(.top, 8)
        .padding(.bottom, 12)
        .frame(maxWidth: .infinity)
        .background(SheetBackground(corner: theme.sheetCorner * (1 - open)))
        // Taps on the sheet's own background must not reach the scrim.
        .contentShape(Rectangle())
        .onTapGesture {}
        .background(HeightReader())
        .onPreferenceChange(HeightKey.self) {
            height = $0
            onSheetHeight($0)
        }
        .onPreferenceChange(SheetContentScrollsKey.self) { contentScrolls = $0 }
        .offset(y: drag + (reduceMotion ? 0 : height * hidden))
        .opacity(reduceMotion ? Double(1 - hidden) : 1)
        // Rows and buttons fill the sheet. As an ordinary gesture the drag waits for them to give the touch up, so the
        // sheet stands still under the finger and then jumps; alongside them the row under the finger, which moved
        // with the sheet, is tapped on release. Taking precedence, the drag cancels the row once it moves.
        // Over content that scrolls, a drag taking precedence would take every pan from the scroll view; there the
        // drag waits for it and moves the sheet from the parts that do not scroll.
        .highPriorityGesture(dragGesture, including: draggable && !contentScrolls ? .all : .subviews)
        .gesture(dragGesture, including: draggable && contentScrolls ? .all : .subviews)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .padding(.top, (insets.top + sheetTopGap) * (1 - open))
        .onAppear(perform: enter)
        .onChange(of: expanded, perform: expand)
    }

    private func enter() {
        let duration = reduceMotion ? SheetMotion.reducedSheetEnterDuration : SheetMotion.sheetEnterDuration
        // The height is known a pass after the first layout; sliding in from zero would show the sheet at once.
        DispatchQueue.main.async {
            // Steps change while the sheet slides in; driven frame by frame they move with it.
            enterAnimator.run(
                duration: duration,
                curve: reduceMotion ? .linear : .sheet,
                step: { hidden = 1 - $0 },
                done: { entered = true }
            )
        }
        if expanded { expand(true) }
    }

    private func expand(_ expanded: Bool) {
        let target: CGFloat = expanded ? 1 : 0
        if reduceMotion {
            open = target
            if expanded { onExpanded() }
            return
        }
        withAnimation(SheetMotion.sheet(SheetMotion.sheetExpandDuration)) { open = target }
        guard expanded else { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + SheetMotion.sheetExpandDuration) {
            // No check of `self.expanded`: this copy of the view may predate the change, and the model checks the step.
            onExpanded()
        }
    }

    private var dragGesture: some Gesture {
        // Global: the sheet moves with the drag, so local coordinates would move under the finger.
        DragGesture(minimumDistance: touchSlop, coordinateSpace: .global)
            // Compose's draggable keeps the touch slop for itself, so its sheet trails the finger by that much.
            .onChanged { drag = max($0.translation.height - touchSlop, 0) }
            .onEnded { value in
                if drag > dismissDistance || velocity(of: value) > dismissVelocity { onDismiss() }
                // The host may keep the sheet (a step inside it goes back instead of closing).
                withAnimation(.interpolatingSpring(stiffness: 1500, damping: 77.5)) { drag = 0 }
            }
    }

    private func velocity(of value: DragGesture.Value) -> CGFloat {
        if #available(iOS 17.0, *) { return value.velocity.height }
        // Before iOS 17 there is no velocity; the predicted end is the velocity run out at UIKit's 0.998 deceleration.
        // It jumps on the last sample of a slow drag, which once closed a sheet released at 100 pt.
        return (value.predictedEndTranslation.height - value.translation.height) / 0.499
    }

    private var touchSlop: CGFloat { 10 }
    private var dismissDistance: CGFloat { 120 }
    private var dismissVelocity: CGFloat { 2400 / UIScreen.main.scale }
}

/// Whether a step in the sheet scrolls its content, so the sheet's drag must leave pans to it.
struct SheetContentScrollsKey: PreferenceKey {
    static let defaultValue = false

    static func reduce(value: inout Bool, nextValue: () -> Bool) {
        value = value || nextValue()
    }
}

struct SheetGrabber: View {
    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        RoundedRectangle(cornerRadius: 2)
            .fill(theme.colors.outlineStrong)
            .frame(width: 38, height: 4)
            .frame(maxWidth: .infinity, minHeight: 14, maxHeight: 14)
            .contentShape(Rectangle())
            .accessibilityHidden(true)
    }
}

/// A rectangle whose top corners are circular arcs, as the Android checkout's sheet shape.
struct TopRoundedRectangle: Shape {
    var radius: CGFloat

    var animatableData: CGFloat {
        get { radius }
        set { radius = newValue }
    }

    func path(in rect: CGRect) -> Path {
        let r = min(radius, rect.width / 2, rect.height / 2)
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.minY + r))
        path.addArc(
            center: CGPoint(x: rect.minX + r, y: rect.minY + r), radius: r,
            startAngle: .degrees(180), endAngle: .degrees(270), clockwise: false
        )
        path.addLine(to: CGPoint(x: rect.maxX - r, y: rect.minY))
        path.addArc(
            center: CGPoint(x: rect.maxX - r, y: rect.minY + r), radius: r,
            startAngle: .degrees(270), endAngle: .degrees(0), clockwise: false
        )
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.closeSubpath()
        return path
    }
}

private struct HeightKey: PreferenceKey {
    static let defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}

private struct HeightReader: View {
    var body: some View {
        GeometryReader { Color.clear.preference(key: HeightKey.self, value: $0.size.height) }
    }
}

/// The sheet's surface with the two shadows the Android checkout's renderer casts for a 24-point elevation: a faint one
/// all around, and a stronger one that a light above the screen's top edge throws downwards, further the lower the
/// sheet starts, so only a tall sheet shows any of it over its top edge.
struct SheetBackground: View {
    let corner: CGFloat

    @Environment(\.checkoutTheme) private var theme

    /// 24 points under a light 600 points up: the shadow is the shape scaled by 600 / 576 from the screen's top.
    private static let throwPerPoint: CGFloat = 24.0 / 576.0
    private static let opacity = 0.11
    private static let blur: CGFloat = 22
    private static let ambientOpacity = 0.05
    private static let ambientBlur: CGFloat = 5

    var body: some View {
        GeometryReader { proxy in
            TopRoundedRectangle(radius: corner)
                .fill(theme.colors.background)
                .shadow(color: Color.black.opacity(SheetBackground.ambientOpacity), radius: SheetBackground.ambientBlur)
                .shadow(
                    color: Color.black.opacity(SheetBackground.opacity),
                    radius: SheetBackground.blur,
                    x: 0,
                    y: max(proxy.frame(in: .global).minY, 0) * SheetBackground.throwPerPoint
                )
        }
    }
}
