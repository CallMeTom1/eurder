import { inject, Pipe, PipeTransform } from '@angular/core';
import { LanguageService } from '@core/i18n/language.service';

const CENTS_PER_EURO = 100;

@Pipe({ name: 'price', pure: false })
export class PricePipe implements PipeTransform {
  private readonly languages = inject(LanguageService);
  private readonly formats = new Map<string, Intl.NumberFormat>();

  transform(cents: number): string {
    return this.formatFor(this.languages.locale()).format(cents / CENTS_PER_EURO);
  }

  private formatFor(locale: string): Intl.NumberFormat {
    let format = this.formats.get(locale);
    if (!format) {
      format = new Intl.NumberFormat(locale, { style: 'currency', currency: 'EUR' });
      this.formats.set(locale, format);
    }
    return format;
  }
}
