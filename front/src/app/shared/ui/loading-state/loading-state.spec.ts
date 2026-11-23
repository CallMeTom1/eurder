import { TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { LoadingState } from './loading-state';

describe('LoadingState', () => {
  it('given a loading page, when rendered, then it shows a spinner and announces the loading politely', async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingState],
      providers: [provideTestTranslations()],
    }).compileComponents();
    const fixture = TestBed.createComponent(LoadingState);

    await fixture.whenStable();

    const status = (fixture.nativeElement as HTMLElement).querySelector('[role="status"]');
    expect(status?.textContent?.trim()).toBe('Loading…');
    expect(status?.querySelector('[role="progressbar"]')).not.toBeNull();
  });
});
