import SwiftUI

/// What a step needs to know about its own change.
struct StepPhase {
    /// The change is over and the sheet stands: native fields stand in for themselves until then.
    var settled = true
    /// The step is coming in under the loading header: only its body appears, see `stepBody()`.
    var reveals = false
    var zoomReveal = false
}

private struct StepPhaseKey: EnvironmentKey {
    static let defaultValue = StepPhase()
}

extension EnvironmentValues {
    var stepPhase: StepPhase {
        get { self[StepPhaseKey.self] }
        set { self[StepPhaseKey.self] = newValue }
    }
}

/// One step at a time in the sheet. A change zooms the whole content: forward, the leaving step grows as it fades and
/// the new one grows in; back, the other way round. The height follows in the same time, the bottom edge staying put.
/// Coming out of loading the layers do not move: the loading layer stays under the new one, whose body fades in.
///
/// Both layers are kept here and driven by one clock instead of SwiftUI transitions: a removal transition is not
/// interpolated reliably, and the leaving step then stood unfaded over the new one until the change ended.
struct StepContainer<Item, Content: View>: View {
    let key: String
    let item: Item
    let forward: Bool
    let revealKey: String?
    let zoomReveal: Bool
    /// Whether a step passes through a light blur while it changes.
    let blurs: (Item) -> Bool
    let reduceMotion: Bool
    /// Whether the sheet is opened to the whole screen for this step: the step then takes its height at once and the
    /// layers hang from the top of the sheet, which is what moves.
    let expanded: Bool
    let sheetEntered: Bool
    @ViewBuilder let content: (Item) -> Content

    /// The step drawn last, kept so that it can leave when the key changes.
    @State private var last: StepLayer<Item>?
    @State private var back: StepLayer<Item>?
    @State private var change = LayerChange()
    @State private var progress: Double = 1
    @State private var height: CGFloat?
    @State private var changeID = 0
    @State private var changeStarted: CFTimeInterval = 0
    /// `onChange` runs with the properties of the previous render, so the step it starts is read from here.
    @State private var current = CurrentStep<Item>()

    var body: some View {
        let _ = current.update(StepLayer(key: key, item: item), forward: forward, revealKey: revealKey, expanded: expanded, blurs: blurs)
        // Until `onChange` has run, the new step is already here but its change has not started: it is drawn away and
        // the step it replaces stays, so the first frame shows no jump.
        let pending = last.map { $0.key != key } ?? false
        let leavingLayer = pending ? last : back
        let reveals = key == revealKey
        let layers = [leavingLayer, StepLayer(key: key, item: item)].compactMap { $0 }
        ZStack(alignment: reveals || expanded ? .top : .bottom) {
            ForEach(layers, id: \.key) { layer in
                let leaving = layer.key != key
                content(layer.item)
                    .environment(
                        \.stepPhase,
                        StepPhase(
                            settled: !leaving && leavingLayer == nil && sheetEntered,
                            reveals: !leaving && reveals,
                            zoomReveal: zoomReveal
                        )
                    )
                    .fixedSize(horizontal: false, vertical: true)
                    .background(StepHeightReader(key: layer.key))
                    .modifier(layerModifier(leaving: leaving, pending: pending))
                    .allowsHitTesting(!leaving)
                    .coveredFromVoiceOver(leaving)
                    .transition(.identity)
            }
        }
        .frame(height: height, alignment: reveals || expanded ? .top : .bottom)
        .clipped()
        .onPreferenceChange(StepHeightKey.self, perform: heightsChanged)
        .onChange(of: key) { _ in stepChanged() }
        .onChange(of: ObjectIdentifier(item as AnyObject)) { _ in remember() }
        .onAppear { remember() }
    }

    private var enterDuration: Double { reduceMotion ? SheetMotion.reducedEnterDuration : SheetMotion.stepDuration }

    private func layerModifier(leaving: Bool, pending: Bool) -> StepChange {
        if change.reveals && !pending || key == revealKey {
            // The layers stand still; only the new step's main button fades in, as its body does.
            return StepChange(progress: leaving ? 1 : (pending ? 0 : progress), exit: leaving, revealOnly: true)
        }
        let shown = pending ? 0 : progress
        return StepChange(
            progress: leaving ? 1 - shown : shown,
            exit: leaving,
            scale: leaving ? change.exitScale : change.enterScale,
            blur: leaving ? change.exitBlur : change.enterBlur
        )
    }

    private func remember() {
        guard let new = current.layer, last?.key == new.key || last == nil else { return }
        last = new
    }

    private func stepChanged() {
        guard let new = current.layer else { return }
        let now = CACurrentMediaTime()
        let shownShare = back == nil ? 1 : (now - changeStarted) / enterDuration
        // A step that took over only moments ago has hardly appeared (Processing gives way to the bank page within
        // tens of milliseconds), so the step before it goes on leaving instead of vanishing in one frame.
        let old = shownShare < 0.5 ? back : last
        changeStarted = now
        let forward = current.forward
        let reveals = new.key == current.revealKey
        let blurOld = old.map { current.blurs?($0.item) ?? false } ?? false
        let blurNew = current.blurs?(new.item) ?? false
        var instant = Transaction()
        instant.disablesAnimations = true
        withTransaction(instant) {
            change = LayerChange(
                reveals: reveals,
                enterScale: reduceMotion ? 1 : (forward ? SheetMotion.zoomIn : SheetMotion.zoomOut),
                exitScale: reduceMotion ? 1 : (forward ? SheetMotion.zoomOut : SheetMotion.zoomIn),
                enterBlur: blurNew && !reduceMotion ? SheetMotion.blur : 0,
                exitBlur: blurOld && !reduceMotion ? SheetMotion.blur : 0
            )
            back = old?.key == new.key ? nil : old
            last = new
            progress = 0
        }
        changeID += 1
        let id = changeID
        DispatchQueue.main.async {
            guard id == changeID else { return }
            withAnimation(.linear(duration: enterDuration)) { progress = 1 }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + enterDuration) {
            guard id == changeID else { return }
            withTransaction(instant) { back = nil }
        }
    }

    private func heightsChanged(_ measured: [String: CGFloat]) {
        guard let target = measured[key], target > 0, target != height else { return }
        if height == nil || reduceMotion || current.expanded {
            height = target
        } else {
            withAnimation(SheetMotion.height()) { height = target }
        }
    }
}

private struct StepLayer<Item> {
    let key: String
    let item: Item
}

private final class CurrentStep<Item> {
    var layer: StepLayer<Item>?
    var forward = true
    var revealKey: String?
    var expanded = false
    var blurs: ((Item) -> Bool)?

    func update(
        _ layer: StepLayer<Item>,
        forward: Bool,
        revealKey: String?,
        expanded: Bool,
        blurs: @escaping (Item) -> Bool
    ) {
        self.layer = layer
        self.expanded = expanded
        self.forward = forward
        self.revealKey = revealKey
        self.blurs = blurs
    }
}

private struct LayerChange {
    var reveals = false
    var enterScale: CGFloat = 1
    var exitScale: CGFloat = 1
    var enterBlur: CGFloat = 0
    var exitBlur: CGFloat = 0
}

/// A step's part of a change, driven linearly so that the fade and the zoom can run on their own clocks: the leaving
/// step fades faster than it grows.
private struct StepChange: AnimatableModifier {
    /// 0 away, 1 present.
    var progress: Double
    let exit: Bool
    var scale: CGFloat = 1
    var blur: CGFloat = 0
    var revealOnly = false

    var animatableData: Double {
        get { progress }
        set { progress = newValue }
    }

    func body(content: Content) -> some View {
        let away = 1 - progress
        let moved = CubicBezier.content.value(at: away)
        let fadeShare = exit ? SheetMotion.stepExitFadeDuration / SheetMotion.stepDuration : 1
        let faded = CubicBezier.content.value(at: min(away / fadeShare, 1))
        let arrived = CubicBezier.content.value(at: progress)
        let alpha = exit ? 1 - faded : arrived
        let zoom = 1 + (scale - 1) * CGFloat(exit ? moved : 1 - arrived)
        return content
            .scaleEffect(revealOnly ? 1 : zoom)
            .opacity(revealOnly ? 1 : alpha)
            .blur(radius: revealOnly ? 0 : blur * CGFloat(exit ? moved : 1 - arrived))
            .primaryButtonChange(alpha: revealOnly && exit ? 1 : alpha, scale: revealOnly ? 1 : zoom)
    }
}

/// The body under a step's header. Coming in over the loading layer it fades in, and when no skeleton was shown before
/// it also grows in out of a light blur, as any other step does.
private struct StepBody: ViewModifier {
    @Environment(\.stepPhase) private var phase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var shown = false

    func body(content: Content) -> some View {
        let reveals = phase.reveals
        let zooms = reveals && phase.zoomReveal && !reduceMotion
        let progress: CGFloat = !reveals || shown ? 1 : 0
        return content
            .scaleEffect(zooms ? SheetMotion.zoomIn + (1 - SheetMotion.zoomIn) * progress : 1)
            .blur(radius: zooms ? SheetMotion.blur * (1 - progress) : 0)
            .opacity(Double(progress))
            .onAppear {
                guard reveals else { return }
                withAnimation(SheetMotion.content(reduceMotion ? SheetMotion.reducedEnterDuration : SheetMotion.stepDuration)) {
                    shown = true
                }
            }
    }
}

extension View {
    func stepBody() -> some View { modifier(StepBody()) }
}

/// A cubic Bézier timing curve through (0,0) and (1,1), evaluated the way CSS and Compose evaluate it.
struct CubicBezier {
    let x1: Double
    let y1: Double
    let x2: Double
    let y2: Double

    static let content = CubicBezier(x1: 0.26, y1: 0.08, x2: 0.25, y2: 1)
    static let label = CubicBezier(x1: 0.22, y1: 0.9, x2: 0.24, y2: 1)
    static let fastOutSlowIn = CubicBezier(x1: 0.4, y1: 0, x2: 0.2, y2: 1)

    func value(at x: Double) -> Double {
        guard x > 0 else { return 0 }
        guard x < 1 else { return 1 }
        var low = 0.0
        var high = 1.0
        var t = x
        for _ in 0..<24 {
            let estimate = CubicBezier.coordinate(t, x1, x2)
            if abs(estimate - x) < 1e-5 { break }
            if estimate < x { low = t } else { high = t }
            t = (low + high) / 2
        }
        return CubicBezier.coordinate(t, y1, y2)
    }

    private static func coordinate(_ t: Double, _ p1: Double, _ p2: Double) -> Double {
        let u = 1 - t
        return 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t
    }
}

private struct StepHeightKey: PreferenceKey {
    static let defaultValue: [String: CGFloat] = [:]

    static func reduce(value: inout [String: CGFloat], nextValue: () -> [String: CGFloat]) {
        value.merge(nextValue()) { $1 }
    }
}

private struct StepHeightReader: View {
    let key: String

    var body: some View {
        GeometryReader { Color.clear.preference(key: StepHeightKey.self, value: [key: $0.size.height]) }
    }
}
