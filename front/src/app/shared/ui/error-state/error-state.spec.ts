import { TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { ErrorState } from './error-state';

describe('ErrorState', () => {
  async function errorState(retryable: boolean) {
    await TestBed.configureTestingModule({
      imports: [ErrorState],
      providers: [provideTestTranslations()],
    }).compileComponents();
    const fixture = TestBed.createComponent(ErrorState);
    fixture.componentRef.setInput('message', 'This item does not exist.');
    fixture.componentRef.setInput('retryable', retryable);
    return fixture;
  }

  it('given an error, when rendered, then the message is announced at once', async () => {
    const fixture = await errorState(false);

    await fixture.whenStable();

    const alert = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('This item does not exist.');
    expect(alert?.querySelector('button')).toBeNull();
  });

  it('given a retryable error, when clicking try again, then it asks to retry', async () => {
    const fixture = await errorState(true);
    const retry = vi.fn();
    fixture.componentInstance.retry.subscribe(retry);
    await fixture.whenStable();
    const button = (fixture.nativeElement as HTMLElement).querySelector('button');

    button?.click();

    expect(button?.textContent?.trim()).toBe('Try again');
    expect(retry).toHaveBeenCalledOnce();
  });
});
