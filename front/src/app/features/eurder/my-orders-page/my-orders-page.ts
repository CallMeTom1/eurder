import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatSnackBar } from '@angular/material/snack-bar';
import { isGlobalProblem } from '@core/api/api-error.interceptor';
import { ApiProblem, isApiProblem, problemOf } from '@core/api/api-problem';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { firstValueFrom } from 'rxjs';
import { EurderReport } from '../eurder-report/eurder-report';
import { EurderService } from '../eurder.service';

const MESSAGE_DURATION = 4000;

@Component({
  imports: [ErrorState, EurderReport, LoadingState, PageHeader, TranslatePipe],
  selector: 'app-my-orders-page',
  templateUrl: './my-orders-page.html',
})
export class MyOrdersPage {
  private readonly eurders = inject(EurderService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  protected readonly report = rxResource({ stream: () => this.eurders.getReport() });
  protected readonly loadProblem = computed(() => problemOf(this.report.error()));
  protected readonly problem = signal<ApiProblem | null>(null);

  protected async reorder(id: string): Promise<void> {
    this.problem.set(null);
    try {
      await firstValueFrom(this.eurders.reorder(id));
      this.snackBar.open(this.translate.instant('eurder.reordered'), undefined, {
        duration: MESSAGE_DURATION,
      });
      this.report.reload();
    } catch (error) {
      if (isApiProblem(error) && !isGlobalProblem(error)) {
        this.problem.set(error);
      }
    }
  }
}
