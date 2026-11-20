@testable import QaApp

extension Decoded {
    func map<U>(_ transform: (T) -> U) -> Decoded<U> {
        switch self {
        case let .ok(value): .ok(transform(value))
        case let .invalid(problem): .invalid(problem)
        }
    }
}
