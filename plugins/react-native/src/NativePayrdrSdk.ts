import { TurboModuleRegistry, type TurboModule } from 'react-native';

/**
 * The native module: JSON strings both ways, the contract of the SDK's `sdk-bridge/README.md`. Apps use the typed API in
 * index.ts instead.
 */
export interface Spec extends TurboModule {
  /** Opens the payment sheet; resolves with the result JSON once it closes. */
  present(requestJson: string): Promise<string>;
  /** SDK Core token or field errors, as JSON. */
  coreToken(requestJson: string): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('PayrdrSdk');
