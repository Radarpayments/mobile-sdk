import React, { useEffect, useRef, type ReactNode } from 'react';
import { Animated, StyleSheet } from 'react-native';

/** Fades its content in when it appears: give it a new `key` to cross from one screen of a store to the next. */
export function FadeIn({ children, duration = 180 }: { children: ReactNode; duration?: number }) {
  const opacity = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    Animated.timing(opacity, { toValue: 1, duration, useNativeDriver: true }).start();
  }, [opacity, duration]);
  return <Animated.View style={[styles.fill, { opacity }]}>{children}</Animated.View>;
}

const styles = StyleSheet.create({ fill: { flex: 1 } });
