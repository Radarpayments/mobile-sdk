import Foundation
import RadarpaymentsSDKPayment

/// The name Android's journal gives the class of [cause]: the Kotlin simple name of a Kotlin class, the type name of a
/// Swift subclass.
///
/// The Objective-C class cannot name a Kotlin cause: one the framework does not export (Ktor's, kotlinx' and the SDK's
/// internal exceptions) arrives as a synthetic class (`RSDKP_kobjcc1`) or as its nearest exported superclass. Its
/// Kotlin `toString()`, which Swift calls `description()`, starts with the qualified name either way.
nonisolated func causeName(of cause: KotlinThrowable) -> String {
    let type = type(of: cause)
    // Only a Swift class has a Swift name apart from its Objective-C one; Kotlin's toString would print the mangled one.
    let swiftName = String(describing: type)
    guard swiftName == NSStringFromClass(type) else { return swiftName }
    return kotlinSimpleName(description: cause.description(), message: cause.message)
}

/// The simple name in a Kotlin `Throwable.toString()`, which is `qualifiedName` or `qualifiedName: message`: what follows
/// the last `.` of the qualified name, `Throwable` when nothing does (Kotlin/Native prints that for an anonymous class).
nonisolated func kotlinSimpleName(description: String, message: String?) -> String {
    var scalars = Array(description.unicodeScalars)
    if let message {
        let suffix = Array((": " + message).unicodeScalars)
        if scalars.count >= suffix.count, scalars[(scalars.count - suffix.count)...].elementsEqual(suffix) {
            scalars.removeLast(suffix.count)
        }
    }
    // A class that overrides toString still starts with the default one (JobCancellationException adds "; job=…").
    if let separator = zip(scalars.indices, scalars.dropFirst()).first(where: { scalars[$0] == ":" && $1 == " " })?.0 {
        scalars.removeSubrange(separator...)
    }
    let name = scalars.split(separator: ".", omittingEmptySubsequences: false).last ?? []
    return name.isEmpty ? "Throwable" : stringOf(name)
}
