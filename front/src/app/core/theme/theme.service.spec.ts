import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  const root = document.documentElement;

  beforeEach(() => {
    localStorage.clear();
    delete root.dataset['theme'];
  });

  it('given no stored choice, when the service starts, then it follows the system', () => {
    const service = TestBed.inject(ThemeService);

    expect(service.preference()).toBe('system');
    expect(root.dataset['theme']).toBeUndefined();
  });

  it('given a stored dark choice, when the service starts, then the page is dark', () => {
    localStorage.setItem('eurder.theme', 'dark');

    const service = TestBed.inject(ThemeService);

    expect(service.preference()).toBe('dark');
    expect(root.dataset['theme']).toBe('dark');
  });

  it('given an unknown stored value, when the service starts, then it follows the system', () => {
    localStorage.setItem('eurder.theme', 'purple');

    const service = TestBed.inject(ThemeService);

    expect(service.preference()).toBe('system');
    expect(root.dataset['theme']).toBeUndefined();
  });

  it('given the service, when choosing light, then the page is light and the choice is stored', () => {
    const service = TestBed.inject(ThemeService);

    service.choose('light');

    expect(service.preference()).toBe('light');
    expect(root.dataset['theme']).toBe('light');
    expect(localStorage.getItem('eurder.theme')).toBe('light');
  });

  it('given a dark page, when choosing the system again, then the page follows the system', () => {
    localStorage.setItem('eurder.theme', 'dark');
    const service = TestBed.inject(ThemeService);

    service.choose('system');

    expect(root.dataset['theme']).toBeUndefined();
    expect(localStorage.getItem('eurder.theme')).toBe('system');
  });
});
