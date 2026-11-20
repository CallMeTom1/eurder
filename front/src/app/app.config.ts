import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { provideRouter } from '@angular/router';
import { DEFAULT_LANGUAGE, LanguageService } from '@core/i18n/language.service';
import { ThemeService } from '@core/theme/theme.service';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideTranslateService({
      loader: provideTranslateHttpLoader({ prefix: '/i18n/', suffix: '.json' }),
      fallbackLang: DEFAULT_LANGUAGE,
    }),
    provideAppInitializer(() => {
      inject(ThemeService);
      inject(MatIconRegistry).setDefaultFontSetClass('material-symbols-outlined');
      return inject(LanguageService).start();
    }),
  ],
};
