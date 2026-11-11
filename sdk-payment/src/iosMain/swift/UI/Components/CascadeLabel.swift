import SwiftUI

/// A piece of a button's label that moves on its own when the label changes.
enum LabelPart: Equatable {
    case icon(PaymentIcon)
    case word(String)
    case spinner
}

/// How a button's label moves on to a new one.
enum LabelMotion {
    /// To the next screen: the whole label turns over to the start side.
    case forward
    /// Back to the screen before: the whole label turns over to the end side.
    case back
    /// On the same screen: the words both labels share stay and glide into place, the others change in a cascade.
    case inPlace
}

/// A button's label as `CascadeLabel` moves it: the icon, then the words of `text`; only a spinner while `loading`.
func labelParts(text: String, icon: PaymentIcon?, loading: Bool) -> [LabelPart] {
    if loading { return [.spinner] }
    return (icon.map { [LabelPart.icon($0)] } ?? []) + cascadeWords(text).map(LabelPart.word)
}

/// The words of `text`. A run isolated left to right (an amount, a card number) stays one word.
func cascadeWords(_ text: String) -> [String] {
    var words: [String] = []
    var word = ""
    var isolated = 0
    for c in text {
        if c == "\u{2066}" { isolated += 1 }
        if c == "\u{2069}" { isolated = max(isolated - 1, 0) }
        if c == " " && isolated == 0 {
            if !word.isEmpty { words.append(word) }
            word = ""
        } else {
            word.append(c)
        }
    }
    if !word.isEmpty { words.append(word) }
    return words
}

/// For each part of `to`, the index of the part of `from` it carries on, or -1: the longest run of parts both labels
/// share, in order.
func matchParts(_ from: [LabelPart], _ to: [LabelPart]) -> [Int] {
    var common = Array(repeating: Array(repeating: 0, count: to.count + 1), count: from.count + 1)
    for i in from.indices.reversed() {
        for j in to.indices.reversed() {
            common[i][j] = from[i] == to[j] ? common[i + 1][j + 1] + 1 : max(common[i + 1][j], common[i][j + 1])
        }
    }
    var match = Array(repeating: -1, count: to.count)
    var i = 0
    var j = 0
    while i < from.count && j < to.count {
        if from[i] == to[j] {
            match[j] = i
            i += 1
            j += 1
        } else if common[i + 1][j] >= common[i][j + 1] {
            i += 1
        } else {
            j += 1
        }
    }
    return match
}

private func fraction(_ elapsed: Double, _ delay: Double, _ duration: Double) -> Double {
    min(max((elapsed - delay) / duration, 0), 1)
}

private func leaveFraction(_ index: Int, _ elapsed: Double) -> Double {
    fraction(elapsed, Double(index) * SheetMotion.labelLeaveStep, SheetMotion.labelLeaveDuration)
}

private func arriveFraction(_ index: Int, _ elapsed: Double, _ duration: Double) -> Double {
    fraction(elapsed, SheetMotion.labelArriveDelay + Double(index) * SheetMotion.labelArriveStep, duration)
}

private struct LabelChange {
    let id = UUID()
    let from: [LabelPart]
    let to: [LabelPart]
    let motion: LabelMotion
    /// Turning over with the screen, nothing carries over: the whole label goes and comes.
    let match: [Int]
    let duration: Double

    init(from: [LabelPart], to: [LabelPart], motion: LabelMotion) {
        self.from = from
        self.to = to
        self.motion = motion
        match = motion == .inPlace ? matchParts(from, to) : Array(repeating: -1, count: to.count)
        if motion != .inPlace {
            duration = max(SheetMotion.labelSlideDuration, SheetMotion.labelSlideInDelay + SheetMotion.labelSlideInFadeDuration)
        } else {
            let kept = match.filter { $0 >= 0 }.count
            let leaving = from.count - kept
            let arriving = to.count - kept
            let cascade = max(
                Double(max(leaving - 1, 0)) * SheetMotion.labelLeaveStep + SheetMotion.labelLeaveDuration,
                SheetMotion.labelArriveDelay + Double(max(arriving - 1, 0)) * SheetMotion.labelArriveStep +
                    SheetMotion.labelArriveMoveDuration
            )
            duration = max(cascade, kept > 0 ? SheetMotion.labelKeepMoveDuration : 0)
        }
    }
}

private struct LabelInput: Equatable {
    let parts: [LabelPart]
    let motion: LabelMotion
}

/// A button's label that moves on when it changes (`LabelMotion`): with a change of screen it turns over sideways, the
/// way the flow went; on the same screen only the words that differ change, one after another, and the rest glide into
/// their new place. At rest, and with reduced motion, `rest` draws the label as it is.
struct CascadeLabel<Rest: View>: View {
    let parts: [LabelPart]
    let style: CheckoutTextStyle
    let color: Color
    /// Where the words' line starts, as `rest` puts it.
    let lineTop: CGFloat
    var motion: LabelMotion = .inPlace
    @ViewBuilder let rest: () -> Rest

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.layoutDirection) private var direction
    @State private var shown: [LabelPart]?
    @State private var change: LabelChange?
    @State private var elapsed: Double = 0

    var body: some View {
        ZStack {
            if let change = change {
                // A modifier rather than an animatable view: SwiftUI steps a modifier's animatable data every frame,
                // where a view with environment values only gets its start and end, and would cross-fade them.
                Color.clear.modifier(
                    MovingLabel(change: change, style: style, color: color, lineTop: lineTop, rtl: direction == .rightToLeft, elapsed: elapsed)
                )
            } else {
                rest()
            }
        }
        .onAppear { if shown == nil { shown = parts } }
        // `onChange` runs with the properties of the previous render, so the motion travels with the parts.
        .onChange(of: LabelInput(parts: parts, motion: motion), perform: moveOn)
    }

    private func moveOn(to input: LabelInput) {
        let new = input.parts
        let motion = input.motion
        guard let old = shown, old != new else { return }
        shown = new
        guard !reduceMotion else {
            change = nil
            return
        }
        let next = LabelChange(from: old, to: new, motion: motion)
        var still = Transaction()
        still.disablesAnimations = true
        withTransaction(still) {
            change = next
            elapsed = 0
        }
        DispatchQueue.main.async {
            withAnimation(.linear(duration: next.duration)) { elapsed = next.duration }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + next.duration) {
            if change?.id == next.id {
                withTransaction(still) { change = nil }
            }
        }
    }
}

/// Both labels of a change laid out as the button lays its label out — the icon `iconGap` before the words, a space
/// between words, centred — each part placed where `elapsed` has taken it. A label wider than the button overflows it
/// on both sides while it changes, rather than squeezing its last words.
private struct MovingLabel: AnimatableModifier {
    let change: LabelChange
    let style: CheckoutTextStyle
    let color: Color
    let lineTop: CGFloat
    let rtl: Bool
    var elapsed: Double

    var animatableData: Double {
        get { elapsed }
        set { elapsed = newValue }
    }

    private struct Placed {
        let part: LabelPart
        let x: CGFloat
        let shiftX: CGFloat
        let shiftY: CGFloat
        let opacity: Double
    }

    func body(content: Content) -> some View {
        GeometryReader { geometry in
            let size = geometry.size
            ZStack(alignment: .topLeading) {
                ForEach(Array(placements(width: size.width).enumerated()), id: \.offset) { _, placed in
                    partView(placed.part)
                        .environment(\.layoutDirection, rtl ? .rightToLeft : .leftToRight)
                        .opacity(placed.opacity)
                        .offset(x: placed.x + placed.shiftX, y: top(of: placed.part, height: size.height) + placed.shiftY)
                }
            }
            .frame(width: size.width, height: size.height, alignment: .topLeading)
            .environment(\.layoutDirection, .leftToRight)
        }
    }

    @ViewBuilder
    private func partView(_ part: LabelPart) -> some View {
        switch part {
        case let .icon(icon): IconView(icon: icon, size: 18, color: color)
        case let .word(text): Text(text).line(style, color: color).fixedSize()
        case .spinner: ButtonSpinner(color: color)
        }
    }

    private func top(of part: LabelPart, height: CGFloat) -> CGFloat {
        switch part {
        case .word: return lineTop
        case .icon: return lineTop + (style.singleLineHeight - 18) / 2
        case .spinner: return (height - 20) / 2
        }
    }

    private func placements(width: CGFloat) -> [Placed] {
        let metrics = LabelMetrics(style: style)
        let arriving = change.to.indices.filter { change.match[$0] < 0 }
        let kept = Set(change.match.filter { $0 >= 0 })
        let scale = UIScreen.main.scale

        func lineUp(_ parts: [LabelPart]) -> (xs: [CGFloat], width: CGFloat) {
            var x: CGFloat = 0
            var xs: [CGFloat] = []
            for (i, part) in parts.enumerated() {
                if i > 0 {
                    if case .word = part, case .word = parts[i - 1] { x += metrics.space } else { x += iconGap }
                }
                xs.append(x)
                x += metrics.width(of: part)
            }
            return (xs, x)
        }
        let (oldXs, oldWidth) = lineUp(change.from)
        let (newXs, newWidth) = lineUp(change.to)
        let oldStart = ((width - oldWidth) * scale / 2).rounded(.down) / scale
        let newStart = ((width - newWidth) * scale / 2).rounded(.down) / scale
        // Positions are laid out from the start side, then mirrored on screen reading right to left.
        func onScreen(_ x: CGFloat, _ part: LabelPart) -> CGFloat { rtl ? width - x - metrics.width(of: part) : x }
        // Forward the label leaves to the start side, as a screen does: to the left, or to the right reading right to left.
        let toStart: CGFloat = rtl ? 1 : -1
        let t = elapsed
        let easing = CubicBezier.label
        let fade = CubicBezier.fastOutSlowIn
        var placed: [Placed] = []

        if change.motion != .inPlace {
            let way = change.motion == .forward ? toStart : -toStart
            let moved = CGFloat(easing.value(at: fraction(t, 0, SheetMotion.labelSlideDuration)))
            let faded = fade.value(at: fraction(t, 0, SheetMotion.labelSlideOutFadeDuration))
            let shown = fade.value(at: fraction(t, SheetMotion.labelSlideInDelay, SheetMotion.labelSlideInFadeDuration))
            for (i, part) in change.from.enumerated() {
                placed.append(Placed(part: part, x: onScreen(oldStart + oldXs[i], part), shiftX: way * SheetMotion.labelSlide * moved, shiftY: 0, opacity: 1 - faded))
            }
            for (j, part) in change.to.enumerated() {
                placed.append(Placed(part: part, x: onScreen(newStart + newXs[j], part), shiftX: -way * SheetMotion.labelSlide * (1 - moved), shiftY: 0, opacity: shown))
            }
            return placed
        }
        let glide = CGFloat(easing.value(at: fraction(t, 0, SheetMotion.labelKeepMoveDuration)))
        var leaving = 0
        for (i, part) in change.from.enumerated() {
            if kept.contains(i), let j = change.match.firstIndex(of: i) {
                let from = oldStart + oldXs[i]
                let to = newStart + newXs[j]
                let x = ((from + (to - from) * glide) * scale).rounded() / scale
                placed.append(Placed(part: part, x: onScreen(x, part), shiftX: 0, shiftY: 0, opacity: 1))
            } else {
                let left = fade.value(at: leaveFraction(leaving, t))
                leaving += 1
                placed.append(Placed(part: part, x: onScreen(oldStart + oldXs[i], part), shiftX: 0, shiftY: -SheetMotion.labelShift * CGFloat(left), opacity: 1 - left))
            }
        }
        for (k, j) in arriving.enumerated() {
            let part = change.to[j]
            let faded = fade.value(at: arriveFraction(k, t, SheetMotion.labelArriveFadeDuration))
            let moved = CGFloat(easing.value(at: arriveFraction(k, t, SheetMotion.labelArriveMoveDuration)))
            placed.append(Placed(part: part, x: onScreen(newStart + newXs[j], part), shiftX: 0, shiftY: SheetMotion.labelShift * (1 - moved), opacity: faded))
        }
        return placed
    }
}

/// Widths of a label's parts in the label's text style, measured as the label's `Text` lays them out.
private struct LabelMetrics {
    let font: UIFont
    let kern: CGFloat

    init(style: CheckoutTextStyle) {
        font = MatchedFont.font(style.sized(style.size))
        kern = style.tracking - MatchedFont.tracking(of: font)
    }

    var space: CGFloat { measure("a a") - measure("aa") }

    func width(of part: LabelPart) -> CGFloat {
        switch part {
        case .icon: return 18
        case .spinner: return 20
        case let .word(text): return measure(text)
        }
    }

    private func measure(_ text: String) -> CGFloat {
        (text as NSString).size(withAttributes: [.font: font, .kern: kern]).width
    }
}
