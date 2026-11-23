import { inject, Injectable } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

const SHOP = 'Eurder';

@Injectable()
export class TranslatedTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly translate = inject(TranslateService);
  private key: string | undefined;

  constructor() {
    super();
    this.translate.onLangChange.pipe(takeUntilDestroyed()).subscribe(() => this.show());
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    this.key = this.buildTitle(snapshot);
    this.show();
  }

  private show(): void {
    this.title.setTitle(this.key ? `${this.translate.instant(this.key)} · ${SHOP}` : SHOP);
  }
}
