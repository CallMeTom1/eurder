import { DOCUMENT, inject, Service, signal } from '@angular/core';

export type ThemePreference = 'light' | 'dark' | 'system';

const STORAGE_KEY = 'eurder.theme';
const PREFERENCES: readonly ThemePreference[] = ['light', 'dark', 'system'];

@Service()
export class ThemeService {
  private readonly root = inject(DOCUMENT).documentElement;
  private readonly chosen = signal<ThemePreference>(storedPreference());

  readonly preference = this.chosen.asReadonly();

  constructor() {
    this.apply(this.chosen());
  }

  choose(preference: ThemePreference): void {
    this.chosen.set(preference);
    localStorage.setItem(STORAGE_KEY, preference);
    this.apply(preference);
  }

  private apply(preference: ThemePreference): void {
    if (preference === 'system') {
      delete this.root.dataset['theme'];
    } else {
      this.root.dataset['theme'] = preference;
    }
  }
}

function storedPreference(): ThemePreference {
  const stored = localStorage.getItem(STORAGE_KEY);
  return PREFERENCES.find((preference) => preference === stored) ?? 'system';
}
