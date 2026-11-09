import { useColorScheme } from 'react-native';

/** Colours of the SDK Core screen; it follows the phone's light or dark theme. */
export interface CoreColors {
  dark: boolean;
  background: string;
  surface: string;
  track: string;
  text: string;
  muted: string;
  placeholder: string;
  border: string;
  accent: string;
  button: string;
  onButton: string;
  error: string;
  success: string;
  successSoft: string;
  step: string;
  stepSoft: string;
}

const dark: CoreColors = {
  dark: true,
  background: '#0B1120',
  surface: '#111827',
  track: '#1F2937',
  text: '#F1F5F9',
  muted: '#94A3B8',
  placeholder: '#64748B',
  border: '#1F2937',
  accent: '#60A5FA',
  button: '#F1F5F9',
  onButton: '#0F172A',
  error: '#F87171',
  success: '#4ADE80',
  successSoft: '#052E16',
  step: '#C7D2FE',
  stepSoft: '#312E81',
};

const light: CoreColors = {
  dark: false,
  background: '#F8FAFC',
  surface: '#FFFFFF',
  track: '#EEF2F6',
  text: '#0F172A',
  muted: '#475569',
  placeholder: '#94A3B8',
  border: '#E2E8F0',
  accent: '#2563EB',
  button: '#0F172A',
  onButton: '#FFFFFF',
  error: '#DC2626',
  success: '#166534',
  successSoft: '#DCFCE7',
  step: '#3730A3',
  stepSoft: '#E0E7FF',
};

export function useCoreColors(): CoreColors {
  return useColorScheme() === 'dark' ? dark : light;
}
