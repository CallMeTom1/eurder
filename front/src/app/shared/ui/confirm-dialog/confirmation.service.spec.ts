import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { ConfirmationService } from './confirmation.service';

@Component({ template: '' })
class HostPage {}

describe('ConfirmationService', () => {
  let loader: HarnessLoader;
  let answers: boolean[];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTestTranslations(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    loader = TestbedHarnessEnvironment.documentRootLoader(TestBed.createComponent(HostPage));
    answers = [];
    TestBed.inject(ConfirmationService)
      .confirm({
        title: 'Delete the chair?',
        message: 'It cannot be undone.',
        confirmLabel: 'Delete',
      })
      .subscribe((answer) => answers.push(answer));
  });

  async function click(text: string): Promise<void> {
    const dialog = await loader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text }))).click();
  }

  it('given a question, when it opens, then the dialog asks it as an alert dialog', async () => {
    const dialog = await loader.getHarness(MatDialogHarness);

    const role = await dialog.getRole();

    expect(role).toBe('alertdialog');
    expect(await dialog.getTitleText()).toBe('Delete the chair?');
    expect(await dialog.getContentText()).toBe('It cannot be undone.');
    expect(await dialog.getActionsText()).toContain('Cancel');
  });

  it('given the dialog, when confirming, then the answer is yes and the dialog closes', async () => {
    await click('Delete');

    expect(answers).toEqual([true]);
    expect(await loader.getAllHarnesses(MatDialogHarness)).toHaveLength(0);
  });

  it('given the dialog, when cancelling, then the answer is no', async () => {
    await click('Cancel');

    expect(answers).toEqual([false]);
  });

  it('given the dialog, when pressing escape, then the answer is no', async () => {
    const dialog = await loader.getHarness(MatDialogHarness);

    await dialog.close();

    expect(answers).toEqual([false]);
  });
});
