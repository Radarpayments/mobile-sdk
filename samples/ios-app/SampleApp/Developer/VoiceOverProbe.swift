import UIKit

/// The elements VoiceOver can reach, for `CheckoutAccessibilityTests`. XCUITest and Accessibility Inspector also list
/// views hidden from VoiceOver and the children of combined elements, so the app walks its own tree the way
/// VoiceOver does. Launched with `-voiceOverProbeDir <dir>`, it answers each `<dir>/<name>.vo-request` with
/// `<dir>/<name>.vo.json`.
enum VoiceOverProbe {
    private static var timer: Timer?

    static func startIfRequested() {
        guard timer == nil, let dir = UserDefaults.standard.string(forKey: "voiceOverProbeDir") else { return }
        let folder = URL(fileURLWithPath: dir)
        timer = Timer.scheduledTimer(withTimeInterval: 0.3, repeats: true) { _ in
            let requests = (try? FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: nil)) ?? []
            for request in requests where request.pathExtension == "vo-request" {
                let name = request.deletingPathExtension().lastPathComponent
                let data = try? JSONSerialization.data(withJSONObject: reachable(), options: [.prettyPrinted])
                try? data?.write(to: folder.appendingPathComponent(name + ".vo.json"))
                try? FileManager.default.removeItem(at: request)
            }
        }
    }

    private static func reachable() -> [[String: Any]] {
        var found: [[String: Any]] = []
        let windows = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .filter { !$0.isHidden }
        modalOnly(windows).forEach { visit($0, into: &found) }
        return found
    }

    private static func visit(_ object: NSObject, into found: inout [[String: Any]]) {
        if let view = object as? UIView, view.isHidden || view.alpha < 0.01 { return }
        if object.accessibilityElementsHidden { return }
        if object.isAccessibilityElement {
            let frame = object.accessibilityFrame
            found.append([
                "label": object.accessibilityLabel ?? "",
                "value": object.accessibilityValue ?? "",
                "traits": object.accessibilityTraits.rawValue,
                "frame": [frame.minX, frame.minY, frame.width, frame.height],
            ])
            return
        }
        modalOnly(children(of: object)).forEach { visit($0, into: &found) }
    }

    private static func children(of object: NSObject) -> [NSObject] {
        if let elements = object.accessibilityElements as? [NSObject], !elements.isEmpty { return elements }
        let count = object.accessibilityElementCount()
        if count != NSNotFound, count > 0 {
            return (0..<count).compactMap { object.accessibilityElement(at: $0) as? NSObject }
        }
        return (object as? UIView)?.subviews ?? []
    }

    /// VoiceOver skips the siblings of a modal view. SwiftUI marks every element of a modal container as modal, so
    /// all modal siblings stay.
    private static func modalOnly(_ siblings: [NSObject]) -> [NSObject] {
        let modal = siblings.filter { $0.accessibilityViewIsModal && !(($0 as? UIView)?.isHidden ?? false) }
        return modal.isEmpty ? siblings : modal
    }
}
