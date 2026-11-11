require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "PayrdrSdk"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  # The SDK supports iOS 14; React Native's own minimum is higher.
  s.platforms    = { :ios => min_ios_version_supported }
  s.source       = { :path => "." }

  # ios/Scanner holds the card scanner's camera in Swift; scripts/build-bridge.sh puts it here.
  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.private_header_files = "ios/**/*.h"
  # The whole SDK as one Kotlin framework; scripts/build-bridge.sh puts it here. It carries the checkout's strings and
  # images inside, so the app needs no build phase.
  s.vendored_frameworks = "ios/Frameworks/PayrdrSdkBridge.xcframework"

  install_modules_dependencies(s)
end
