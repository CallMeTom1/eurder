import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { MatIconRegistry } from '@angular/material/icon';
import { provideRouter, TitleStrategy, withComponentInputBinding } from '@angular/router';
import { apiErrorInterceptor } from '@core/api/api-error.interceptor';
import { DEFAULT_LANGUAGE, LanguageService } from '@core/i18n/language.service';
import { TranslatedTitleStrategy } from '@core/i18n/translated-title-strategy';
import { authInterceptor } from '@core/security/auth.interceptor';
import { SecurityService } from '@core/security/security.service';
import { SessionWatcher } from '@core/security/session-watcher';
import { ThemeService } from '@core/theme/theme.service';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { concat } from 'rxjs';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    { provide: TitleStrategy, useClass: TranslatedTitleStrategy },
    provideHttpClient(
      withInterceptors([apiErrorInterceptor, authInterceptor]),
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
    ),
    provideTranslateService({
      loader: provideTranslateHttpLoader({ prefix: '/i18n/', suffix: '.json' }),
      fallbackLang: DEFAULT_LANGUAGE,
    }),
    provideAppInitializer(() => {
      inject(ThemeService);
      inject(SessionWatcher);
      inject(MatIconRegistry).setDefaultFontSetClass('material-symbols-outlined');
      const language = inject(LanguageService);
      const security = inject(SecurityService);
      return concat(language.start(), security.loadCurrentUser());
    }),
  ],
};
