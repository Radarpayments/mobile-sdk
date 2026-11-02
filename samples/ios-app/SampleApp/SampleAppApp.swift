import Combine
import SwiftUI
import sdk_payment_scanner

enum SampleRoute: Hashable {
    case developer
    case core
    case paymentAdvanced, paymentHeadless
    case paymentResult
    case cover(CoverId)
    case result(CoverId)
}

/// Shared nav path so screens can push and pop programmatically.
final class PathHolder: ObservableObject {
    @Published var path: [SampleRoute] = []
    func push(_ r: SampleRoute) { path.append(r) }
    /// Back from the Advanced demo's result screen.
    func popToPaymentRoot() { if !path.isEmpty { path.removeLast() } }
}

@main
struct SampleAppApp: App {
    @StateObject private var store = SampleStore()
    @StateObject private var storeModel = StoreModel()
    @AppStorage(AppLanguage.key) private var language = ""
    @StateObject private var pathHolder: PathHolder = {
        // The last business opens straight away.
        let holder = PathHolder()
        let prefs = StorePrefs()
        let aftDemo = isAftDemo(apiVersion: prefs.apiVersion, orderType: prefs.orderType)
        if let last = reopenCover(prefs.lastCover, aftDemo: aftDemo) { holder.path = [.cover(last)] }
        return holder
    }()

    init() {
        IosCrashLoggerKt.installIosCrashLogger()
        NSSetUncaughtExceptionHandler { exception in
            NSLog("[ObjCUncaught] name=%@ reason=%@", exception.name.rawValue as NSString, (exception.reason ?? "<nil>") as NSString)
            for frame in exception.callStackSymbols {
                NSLog("[ObjCUncaught stack] %@", frame as NSString)
            }
        }
        signal(SIGABRT) { sig in
            NSLog("[SigAbrt] received SIGABRT")
            signal(SIGABRT, SIG_DFL)
            raise(SIGABRT)
        }
    }

    var body: some Scene {
        WindowGroup {
            NavigationStack(path: $pathHolder.path) {
                StartView(
                    onCover: { cover in
                        StorePrefs().lastCover = cover
                        pathHolder.push(.cover(cover))
                    },
                    onSettings: { pathHolder.push(.developer) }
                )
                .navigationDestination(for: SampleRoute.self) { route in
                    switch route {
                    case .developer: DeveloperView()
                    case .core: SdkCoreView()
                    case .paymentAdvanced: LightSampleChrome { AdvancedView() }
                    case .paymentHeadless: LightSampleChrome { HeadlessFlowView() }
                    case .paymentResult: LightSampleChrome { PaymentResultView() }
                    case let .cover(cover): CoverRoot(cover: cover)
                    case let .result(cover): CoverResult(cover: cover)
                    }
                }
            }
            // Back from the settings in the other mode: the business open under them no longer fits it.
            .onChange(of: pathHolder.path) { path in
                let prefs = StorePrefs()
                let fitting = pathFitting(path, aftDemo: isAftDemo(apiVersion: prefs.apiVersion, orderType: prefs.orderType))
                if fitting != path { pathHolder.path = fitting }
            }
            .appLanguage(language.isEmpty ? nil : language)
            .environmentObject(store)
            .environmentObject(storeModel)
            .environmentObject(pathHolder)
        }
    }
}
