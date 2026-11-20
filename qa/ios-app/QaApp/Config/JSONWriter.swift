nonisolated extension JSONValue {
    /// The value as compact JSON, members in their order.
    var text: String {
        var output = ""
        write(to: &output, indent: nil)
        return output
    }

    /// The value as JSON indented by four spaces, as Android's kotlinx writes it with `prettyPrint`.
    var prettyText: String {
        var output = ""
        write(to: &output, indent: "")
        return output
    }

    private func write(to output: inout String, indent: String?) {
        switch self {
        case .null: output += "null"
        case let .bool(value): output += value ? "true" : "false"
        case let .number(text): output += text
        case let .string(text): writeQuoted(text, to: &output)
        case let .array(items):
            writeList(items, open: "[", close: "]", to: &output, indent: indent) { item, output, inner in
                item.write(to: &output, indent: inner)
            }
        case let .object(object):
            writeList(object.members, open: "{", close: "}", to: &output, indent: indent) { member, output, inner in
                writeQuoted(member.key, to: &output)
                output += inner == nil ? ":" : ": "
                member.value.write(to: &output, indent: inner)
            }
        }
    }

    private func writeList<Item>(
        _ items: [Item],
        open: String,
        close: String,
        to output: inout String,
        indent: String?,
        writeItem: (Item, inout String, String?) -> Void
    ) {
        output += open
        guard !items.isEmpty else { return output += close }
        let inner = indent.map { $0 + "    " }
        for (position, item) in items.enumerated() {
            if position > 0 { output += "," }
            if let inner { output += "\n" + inner }
            writeItem(item, &output, inner)
        }
        if let indent { output += "\n" + indent }
        output += close
    }

    private func writeQuoted(_ text: String, to output: inout String) {
        output += "\""
        for scalar in text.unicodeScalars {
            switch scalar {
            case "\"": output += "\\\""
            case "\\": output += "\\\\"
            case "\n": output += "\\n"
            case "\r": output += "\\r"
            case "\t": output += "\\t"
            case "\u{08}": output += "\\b"
            case "\u{0C}": output += "\\f"
            case "\u{00}" ..< "\u{20}":
                let hex = String(scalar.value, radix: 16)
                output += "\\u" + String(repeating: "0", count: 4 - hex.count) + hex
            default: output.unicodeScalars.append(scalar)
            }
        }
        output += "\""
    }
}
