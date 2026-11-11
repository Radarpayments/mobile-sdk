#import "PayrdrSdk.h"
#import <PayrdrSdkBridge/PayrdrSdkBridge.h>
// After the bridge's header: the Swift header names its protocols without importing them in Objective-C++.
#if __has_include(<PayrdrSdk/PayrdrSdk-Swift.h>)
#import <PayrdrSdk/PayrdrSdk-Swift.h>
#else
#import "PayrdrSdk-Swift.h"
#endif

// Passes the JavaScript side's JSON to the SDK's bridge and its answers back; all mapping lives in sdk-bridge. The sheet
// opens over whatever the app's key window shows.
@implementation PayrdrSdk {
  PSBPayrdrBridgeCheckout *_checkout;
}

- (instancetype)init
{
  if (self = [super init]) {
    _checkout = [[PSBPayrdrBridgeCheckout alloc] initWithScannerCamera:[VisionScannerCamera new]];
  }
  return self;
}

- (void)present:(NSString *)requestJson resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject
{
  dispatch_async(dispatch_get_main_queue(), ^{
    [self->_checkout presentRequestJson:requestJson from:nil onResult:^(NSString *result) { resolve(result); }];
  });
}

- (void)coreToken:(NSString *)requestJson resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject
{
  resolve([PSBPayrdrBridge.shared coreTokenRequestJson:requestJson]);
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params
{
  return std::make_shared<facebook::react::NativePayrdrSdkSpecJSI>(params);
}

+ (NSString *)moduleName
{
  return @"PayrdrSdk";
}

@end
