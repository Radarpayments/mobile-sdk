#import "PayrdrSdk.h"
#import <PayrdrSdkBridge/PayrdrSdkBridge.h>

// Passes the JavaScript side's JSON to the SDK's bridge and its answers back; all mapping lives in sdk-bridge. The sheet
// opens over whatever the app's key window shows.
@implementation PayrdrSdk {
  PSBPayrdrBridgeCheckout *_checkout;
}

- (instancetype)init
{
  if (self = [super init]) {
    _checkout = [PSBPayrdrBridgeCheckout new];
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
