import { Provider } from '@angular/core';
import { provideTranslateService, TranslateLoader, TranslationObject } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import en from '../../../../public/i18n/en.json';
import fr from '../../../../public/i18n/fr.json';
import nl from '../../../../public/i18n/nl.json';
import { DEFAULT_LANGUAGE, Language } from './language.service';

const TRANSLATIONS: Record<Language, TranslationObject> = { en, fr, nl };

class FileTranslateLoader implements TranslateLoader {
  getTranslation(language: string): Observable<TranslationObject> {
    return of(TRANSLATIONS[language as Language]);
  }
}

export function provideTestTranslations(language: Language = DEFAULT_LANGUAGE): Provider[] {
  return provideTranslateService({
    loader: { provide: TranslateLoader, useClass: FileTranslateLoader },
    fallbackLang: DEFAULT_LANGUAGE,
    lang: language,
  });
}
