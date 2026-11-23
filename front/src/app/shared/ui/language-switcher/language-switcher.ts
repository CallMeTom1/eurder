import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { Language, LanguageService } from '@core/i18n/language.service';
import { TranslatePipe } from '@ngx-translate/core';

interface LanguageOption {
  language: Language;
  name: string;
}

@Component({
  imports: [MatButtonModule, MatMenuModule, TranslatePipe],
  selector: 'app-language-switcher',
  templateUrl: './language-switcher.html',
})
export class LanguageSwitcher {
  protected readonly languages = inject(LanguageService);

  protected readonly options: LanguageOption[] = [
    { language: 'fr', name: 'Français' },
    { language: 'en', name: 'English' },
    { language: 'nl', name: 'Nederlands' },
  ];
}
