import Foundation

/// String identifiers used with @AppStorage — kept centralised so Android and iOS samples
/// share the same persistence namespace conceptually.
enum AppPrefsKeys {
    static let baseUrl = "base_url"
    static let mdOrder = "md_order"
    static let pubKeyPem = "pub_key_pem"
    static let bindingId = "core_bid"
    static let sdkCoreVariant = "core_var"
    static let apayMerchantId = "apay_mid"
}

/// Hard-coded default values. When @AppStorage does not find a value, it falls back here.
enum AppPrefsDefaults {
    static let baseUrl = "https://dev.bpcbt.com/payment"
    static let mdOrder = "c400b41a-aa3d-43db-8727-ac4ca9e8f701"
    static let bindingId = "513b17f4-e32e-414f-8c74-936fd7027baa"
    static let sdkCoreVariant = "new"
    static let apayMerchantId = "merchant.net.payrdr.samples.test"
    static let pubKeyPem = """
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAij/G3JVV3TqYCFZTPmwi
4JduQMsZ2HcFLBBA9fYAvApv3FtA+zKdUGgKh/OPbtpsxe1C57gIaRclbzMoafTb
0eOdj+jqSEJMlVJYSiZ8Hn6g67evhu9wXh5ZKBQ1RUpqL36LbhYnIrP+TEGR/Vyj
bC6QTfaktcRfa8zRqJczHFsyWxnlfwKLfqKz5wSqXkShcrwcfRJCyDRjZX6OFUEC
HsWVK3WMcOV3WZREwbCkh/o5R5Vl6xoyLvSqVEKQiHupJcZu9UEOJiP3yNCn9YPg
yFs2vrCeg6qxDPFnCfetcDCLjjLenGF7VyZzBJ9G2NP3k/mNVtD8Kl7lpiurwY7E
ZwIDAQAB
-----END PUBLIC KEY-----
"""
}
