// The native module as the tests see it: every call is recorded and answered with `nativeReply`.
export const calls: Array<{ method: string; request: Record<string, unknown> }> = [];
export const native = { reply: {} as unknown };

const answer = (method: string) => async (requestJson: string) => {
  calls.push({ method, request: JSON.parse(requestJson) as Record<string, unknown> });
  return JSON.stringify(native.reply);
};

export const TurboModuleRegistry = {
  getEnforcing: <T>(_name: string) => ({ present: answer('present'), coreToken: answer('coreToken') }) as unknown as T,
};

// What the spec extends; the real one comes from react-native.
export interface TurboModule {}
