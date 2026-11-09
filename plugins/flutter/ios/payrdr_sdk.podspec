# CocoaPods form of the plugin; payrdr_sdk/Package.swift is the Swift Package Manager form of the same sources and framework.
require 'yaml'
pubspec = YAML.load_file(File.join(__dir__, '..', 'pubspec.yaml'))

Pod::Spec.new do |s|
  s.name             = 'payrdr_sdk'
  # The plugin's version is the one in pubspec.yaml.
  s.version          = pubspec['version']
  s.summary          = 'payrdr payment SDK for Flutter.'
  s.description      = <<-DESC
The native payrdr payment sheet (3-D Secure, Apple Pay, card scanner) and SDK Core card encryption for Flutter apps.
                       DESC
  s.homepage         = 'https://payrdr.net'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'payrdr' => 'mobile@payrdr.net' }
  s.source           = { :path => '.' }
  s.source_files     = 'payrdr_sdk/Sources/payrdr_sdk/**/*.swift'
  s.resource_bundles = { 'payrdr_sdk_privacy' => ['payrdr_sdk/Sources/payrdr_sdk/PrivacyInfo.xcprivacy'] }
  # The whole SDK as one Kotlin framework; scripts/build-bridge.sh puts it here.
  s.vendored_frameworks = 'payrdr_sdk/Frameworks/PayrdrSdkBridge.xcframework'
  s.dependency 'Flutter'
  s.platform = :ios, '15.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
