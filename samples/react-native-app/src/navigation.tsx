import React, { createContext, useContext, useEffect, useMemo, useRef, useState, type ComponentType } from 'react';
import { Animated, BackHandler, Platform, StyleSheet, useWindowDimensions } from 'react-native';

import type { CheckoutOutcome } from './checkout/checkout';
import type { CoverId } from './store/model/catalog';
import type { Receipt } from './store/storeModel';

/*
 * A small screen stack of the demo's own. React Navigation's native stack needs react-native-screens, which asks
 * MainActivity to install its fragment factory; the demo keeps MainActivity a plain ReactActivity instead, to show
 * that the payment SDK needs nothing there.
 */

/** The screens of the demo and what each one is opened with. */
export type RootStackParamList = {
  Start: undefined;
  /** The screens of one business; each cover's own flow switches inside it. */
  Cover: { cover: CoverId };
  /** A screen of its own, so that it fades over the catalogue. */
  FurnitureProduct: { productId: string };
  /** `orderNumber`: what the coffee shop calls out. */
  StoreResult: { cover: CoverId; receipt: Receipt; orderNumber: number };
  Settings: undefined;
  Core: undefined;
};

export type RouteName = keyof RootStackParamList;

export interface Route<Name extends RouteName = RouteName> {
  key: string;
  name: Name;
  params: RootStackParamList[Name];
}

export interface Navigation {
  push<Name extends RouteName>(
    name: Name,
    ...params: RootStackParamList[Name] extends undefined ? [] : [RootStackParamList[Name]]
  ): void;
  /** Back, as the system Back does it: a screen that intercepts it (`useBackHandler`) handles it instead. */
  goBack(): void;
  /** Closes the screen on top whatever it intercepts: the screen itself has to go, not a step inside it. */
  leave(): void;
}

export interface ScreenProps<Name extends RouteName> {
  navigation: Navigation;
  route: Route<Name>;
}

/** How a store hands a finished payment of `amountMinor` to the app. */
export type OnOutcome = (outcome: CheckoutOutcome, amountMinor: number) => void;

export type Screens = { [Name in RouteName]: ComponentType<ScreenProps<Name>> };

/** `fade` for a screen that shows more of the one below it, `push` for the next step. */
export type Transition = 'push' | 'fade';

interface Entry extends Route {
  leaving: boolean;
}

interface StackContext {
  navigation: Navigation;
  interceptors: Map<string, () => void>;
  /** Goes back if it can; false at the first screen, where the system Back leaves the app. */
  back(): boolean;
}

const Context = createContext<StackContext | undefined>(undefined);
const RouteKey = createContext('');
const IsTop = createContext(true);

function useStack(): StackContext {
  const stack = useContext(Context);
  if (!stack) throw new Error('Outside StackNavigator');
  return stack;
}

export function useNavigation(): Navigation {
  return useStack().navigation;
}

/** Whether this screen is the one on top (not covered by a screen pushed over it). */
export function useIsTop(): boolean {
  return useContext(IsTop);
}

/** While `active`, Back on this screen calls `handler` instead of closing it, e.g. from the cart to the catalogue. */
export function useBackHandler(active: boolean, handler: () => void): void {
  const { interceptors } = useStack();
  const key = useContext(RouteKey);
  const latest = useRef(handler);
  useEffect(() => {
    latest.current = handler;
  });
  useEffect(() => {
    if (!active) return;
    const intercept = () => latest.current();
    interceptors.set(key, intercept);
    return () => {
      if (interceptors.get(key) === intercept) interceptors.delete(key);
    };
  }, [active, interceptors, key]);
}

export interface StackNavigatorProps {
  screens: Screens;
  initial: RouteName;
  transitions?: Partial<Record<RouteName, Transition>>;
  rtl: boolean;
}

/** The screens on top of each other: those below keep their state, only the top one takes touches. */
export function StackNavigator({ screens, initial, transitions = {}, rtl }: StackNavigatorProps) {
  const [entries, setEntries] = useState<Entry[]>([
    { key: 'route-0', name: initial, params: undefined, leaving: false },
  ]);
  const current = useRef(entries);
  useEffect(() => {
    current.current = entries;
  });
  const [stack] = useState<StackContext>(() => {
    let nextKey = 1;
    const interceptors = new Map<string, () => void>();
    const navigation: Navigation = {
      push: (name, ...params) =>
        setEntries(list => [...list, { key: `route-${nextKey++}`, name, params: params[0], leaving: false } as Entry]),
      goBack: () => {
        back();
      },
      leave: () => {
        const open = current.current.filter(entry => !entry.leaving);
        if (open.length < 2) return;
        const top = open[open.length - 1];
        setEntries(list => list.map(entry => (entry.key === top.key ? { ...entry, leaving: true } : entry)));
      },
    };
    const back = (): boolean => {
      const open = current.current.filter(entry => !entry.leaving);
      const top = open[open.length - 1];
      if (open.length < 2) return false;
      const intercept = interceptors.get(top.key);
      if (intercept) intercept();
      else setEntries(list => list.map(entry => (entry.key === top.key ? { ...entry, leaving: true } : entry)));
      return true;
    };
    return { navigation, interceptors, back };
  });
  useEffect(() => {
    const subscription = BackHandler.addEventListener('hardwareBackPress', stack.back);
    return () => subscription.remove();
  }, [stack]);

  const open = entries.filter(entry => !entry.leaving);
  const topKey = open[open.length - 1].key;
  return (
    <Context.Provider value={stack}>
      {entries.map((entry, index) => {
        const Screen = screens[entry.name] as ComponentType<ScreenProps<RouteName>>;
        return (
          <Frame
            key={entry.key}
            animated={index > 0}
            leaving={entry.leaving}
            top={entry.key === topKey}
            transition={transitions[entry.name] ?? 'push'}
            rtl={rtl}
            onLeft={() => setEntries(list => list.filter(other => other.key !== entry.key))}
          >
            <RouteKey.Provider value={entry.key}>
              <IsTop.Provider value={entry.key === topKey}>
                <Screen navigation={stack.navigation} route={entry} />
              </IsTop.Provider>
            </RouteKey.Provider>
          </Frame>
        );
      })}
    </Context.Provider>
  );
}

interface FrameProps {
  animated: boolean;
  leaving: boolean;
  top: boolean;
  transition: Transition;
  rtl: boolean;
  onLeft: () => void;
  children: React.ReactNode;
}

const DURATION_MS = Platform.select({ ios: 350, default: 300 });

/**
 * One screen of the stack. It comes in and goes out the platform's way: from the side on iOS (the reading
 * direction's side), rising and fading on Android.
 */
function Frame({ animated, leaving, top, transition, rtl, onLeft, children }: FrameProps) {
  const { width } = useWindowDimensions();
  const [shown] = useState(() => new Animated.Value(animated ? 0 : 1));
  const left = useRef(onLeft);
  useEffect(() => {
    left.current = onLeft;
  });
  useEffect(() => {
    Animated.timing(shown, { toValue: leaving ? 0 : 1, duration: DURATION_MS, useNativeDriver: true }).start(
      ({ finished }) => {
        if (finished && leaving) left.current();
      },
    );
  }, [leaving, shown]);

  const motion = useMemo(() => {
    if (transition === 'fade') return { opacity: shown };
    if (Platform.OS === 'ios') {
      return {
        transform: [{ translateX: shown.interpolate({ inputRange: [0, 1], outputRange: [rtl ? -width : width, 0] }) }],
      };
    }
    return {
      opacity: shown,
      transform: [{ translateY: shown.interpolate({ inputRange: [0, 1], outputRange: [48, 0] }) }],
    };
  }, [rtl, shown, transition, width]);

  return (
    <Animated.View
      style={[StyleSheet.absoluteFill, motion]}
      pointerEvents={top ? 'auto' : 'none'}
      importantForAccessibility={top ? 'auto' : 'no-hide-descendants'}
      accessibilityElementsHidden={!top}
    >
      {children}
    </Animated.View>
  );
}
