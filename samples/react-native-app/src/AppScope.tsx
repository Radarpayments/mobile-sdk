import React, { createContext, useContext, type ReactNode } from 'react';

import type { PresentSheet } from './checkout/checkout';
import { formatEur, isRtl, stringsFor, systemLanguage, type LanguageCode, type Strings } from './l10n/l10n';
import { useListenable } from './listenable';
import { merchantRegistrar, type OrderRegistrar, type TransferPrefill } from './merchant/orderRegistrar';
import type { AppPrefs } from './prefs/appPrefs';
import type { StoreModel } from './store/storeModel';

/** Builds the "your server" registrar from the developer settings; tests provide a fake. */
export type RegistrarFactory = (prefs: AppPrefs) => OrderRegistrar;

export const gatewayRegistrar: RegistrarFactory = prefs =>
  merchantRegistrar({
    apiVersion: prefs.apiVersion,
    orderType: prefs.orderType,
    baseUrl: prefs.baseUrl,
    login: prefs.merchantLogin,
    password: prefs.merchantPassword,
    apiKey: prefs.apiKey,
  });

/** "Your server" for Northwind Invest: the same, but a transfer is registered with the investor's details. */
export type TransferRegistrarFactory = (prefs: AppPrefs, prefill: TransferPrefill) => OrderRegistrar;

export const gatewayTransferRegistrar: TransferRegistrarFactory = (prefs, prefill) =>
  merchantRegistrar({
    apiVersion: prefs.apiVersion,
    orderType: prefs.orderType,
    baseUrl: prefs.baseUrl,
    login: prefs.merchantLogin,
    password: prefs.merchantPassword,
    apiKey: prefs.apiKey,
    prefill,
  });

/**
 * What every screen shares: the settings, the carts, "your server" and the way the sheet opens. The objects stay the
 * same for the whole app; screens listen to them for changes.
 */
export interface AppScopeValue {
  prefs: AppPrefs;
  store: StoreModel;
  registrarFactory: RegistrarFactory;
  /** Northwind's registrar; the gateway one unless a test passes a fake. */
  transferRegistrarFactory?: TransferRegistrarFactory;
  /** Replaces the SDK sheet in tests; undefined opens the real one. */
  presentSheet?: PresentSheet;
}

const Scope = createContext<AppScopeValue | undefined>(undefined);

export function AppScope({ value, children }: { value: AppScopeValue; children: ReactNode }) {
  return <Scope.Provider value={value}>{children}</Scope.Provider>;
}

export function useAppScope(): AppScopeValue {
  const scope = useContext(Scope);
  if (!scope) throw new Error('useAppScope outside AppScope');
  return scope;
}

/** The settings; the component re-renders when they change. */
export function usePrefs(): AppPrefs {
  return useListenable(useAppScope().prefs);
}

/** The carts; the component re-renders when they change. */
export function useStore(): StoreModel {
  return useListenable(useAppScope().store);
}

export interface L10n {
  strings: Strings;
  language: LanguageCode;
  rtl: boolean;
  /** Euro cents in the demo's language. */
  eur(minor: number): string;
}

/** The demo's language: the one picked in the developer settings, or the device's. */
export function useL10n(): L10n {
  const language = usePrefs().localeOverride ?? systemLanguage();
  return { strings: stringsFor(language), language, rtl: isRtl(language), eur: minor => formatEur(minor, language) };
}
