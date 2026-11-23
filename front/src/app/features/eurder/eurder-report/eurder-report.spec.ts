import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { Report } from '../report';
import { EurderReport } from './eurder-report';

@Component({
  imports: [EurderReport],
  template: `
    <app-eurder-report
      [report]="report()"
      [canReorder]="canReorder()"
      (reorder)="asked.push($event)"
    />
  `,
})
class HostPage {
  readonly report = signal<Report>({ eurders: [], totalInCents: 0 });
  readonly canReorder = signal(false);
  readonly asked: string[] = [];
}

describe('EurderReport', () => {
  const report: Report = {
    eurders: [
      {
        id: 'e1',
        lines: [
          { itemName: 'Chair', itemState: 'PRISTINE', amount: 2, subtotalInCents: 9998 },
          { itemName: 'Lamp', itemState: 'DAMAGED', amount: 1, subtotalInCents: 2610 },
        ],
        totalInCents: 12608,
      },
      {
        id: 'e2',
        lines: [{ itemName: 'Lamp', itemState: 'PRISTINE', amount: 1, subtotalInCents: 2900 }],
        totalInCents: 2900,
      },
    ],
    totalInCents: 15508,
  };

  async function render(given: Report, canReorder = false) {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [HostPage],
      providers: [provideTestTranslations()],
    }).compileComponents();
    const fixture = TestBed.createComponent(HostPage);
    fixture.componentInstance.report.set(given);
    fixture.componentInstance.canReorder.set(canReorder);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  it('given a report, when rendered, then each order lists its lines with a damaged state shown, and the totals', async () => {
    const { element } = await render(report);

    const orders = element.querySelectorAll('[data-testid="eurder"]');

    expect(orders).toHaveLength(2);
    expect(orders[0].querySelector('h3')?.textContent?.trim()).toBe('Order 1');
    expect(orders[0].textContent).toContain('Chair');
    expect(orders[0].textContent).toContain('Lamp');
    expect(orders[0].textContent).toContain('Damaged');
    expect(orders[0].textContent).not.toContain('New');
    expect(orders[0].textContent).toContain('€126.08');
    expect(element.querySelector('[data-testid="report-total"]')?.textContent).toContain('€155.08');
  });

  it('given no order, when rendered, then it says so', async () => {
    const { element } = await render({ eurders: [], totalInCents: 0 });

    const text = element.textContent;

    expect(text).toContain('No order yet.');
  });

  it('given a report for its customer, when asking to order again, then it tells which order', async () => {
    const { fixture, element } = await render(report, true);

    element.querySelectorAll<HTMLButtonElement>('[data-testid="eurder"] button')[1].click();

    expect(fixture.componentInstance.asked).toEqual(['e2']);
  });

  it('given a report read by the admin, when rendered, then there is no button to order again', async () => {
    const { element } = await render(report);

    const button = element.querySelector('button');

    expect(button).toBeNull();
  });
});
