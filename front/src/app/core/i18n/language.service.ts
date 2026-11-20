import { DOCUMENT, inject, Service, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

export type Language = 'fr' | 'en' | 'nl';

export const LANGUAGES: readonly Language[] = ['fr', 'en', 'nl'];
export const DEFAULT_LANGUAGE: Language = 'en';

const STORAGE_KEY = 'eurder.lang';

@Service()
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly root = inject(DOCUMENT).documentElement;
  private readonly current = signal<Language>(initialLanguage());

  readonly language = this.current.asReadonly();

  start(): Observable<unknown> {
    return this.apply(this.current());
  }

  choose(language: Language): void {
    this.current.set(language);
    localStorage.setItem(STORAGE_KEY, language);
    this.apply(language).subscribe();
  }

  private apply(language: Language): Observable<unknown> {
    this.root.lang = language;
    return this.translate.use(language);
  }
}

function initialLanguage(): Language {
  return (
    supported(localStorage.getItem(STORAGE_KEY)) ??
    navigator.languages.map((tag) => supported(tag.slice(0, 2))).find((language) => !!language) ??
    DEFAULT_LANGUAGE
  );
}

function supported(candidate: string | null): Language | undefined {
  return LANGUAGES.find((language) => language === candidate);
}
