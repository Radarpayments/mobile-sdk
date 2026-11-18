import Foundation
import RadarpaymentsSDKPayment

/// The name Android's `simpleName` gives the class of [object] when it is a Kotlin class of the SDK framework, its Swift
/// type name otherwise.
///
/// Swift prints a Kotlin class by its Objective-C name: the framework prefix, then `Kotlin` for the standard library
/// (`RSDKPKotlinIllegalStateException`). Both are dropped so that a journal line reads the same on both platforms.
nonisolated func typeName(of object: AnyObject) -> String {
    let name = NSStringFromClass(type(of: object))
    guard !frameworkPrefix.isEmpty, name.hasPrefix(frameworkPrefix) else { return String(describing: type(of: object)) }
    let kotlinName = name.dropFirst(frameworkPrefix.count)
    let standardName = kotlinName.hasPrefix(standardLibraryPrefix) ? kotlinName.dropFirst(standardLibraryPrefix.count) : kotlinName
    return String(standardName)
}

nonisolated private let standardLibraryPrefix = "Kotlin"

nonisolated private let frameworkPrefix: String = {
    let throwable = NSStringFromClass(KotlinThrowable.self)
    let swiftName = "KotlinThrowable"
    return throwable.hasSuffix(swiftName) ? String(throwable.dropLast(swiftName.count)) : ""
}()
