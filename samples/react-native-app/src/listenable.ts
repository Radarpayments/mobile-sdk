import { useSyncExternalStore } from 'react';

/**
 * A model screens listen to, like Flutter's ChangeNotifier: getters read its current state, every change bumps
 * `version` and tells the listeners.
 */
export class Listenable {
  private listeners = new Set<() => void>();
  private changes = 0;

  get version(): number {
    return this.changes;
  }

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  protected notify(): void {
    this.changes++;
    this.listeners.forEach(listener => listener());
  }
}

/** Re-renders the component on every change of `model` and gives it back. */
export function useListenable<T extends Listenable>(model: T): T {
  useSyncExternalStore(model.subscribe, () => model.version);
  return model;
}
