import { inject, Service } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { map, Observable } from 'rxjs';
import { Confirmation, ConfirmDialog } from './confirm-dialog';

@Service()
export class ConfirmationService {
  private readonly dialog = inject(MatDialog);

  confirm(confirmation: Confirmation): Observable<boolean> {
    return this.dialog
      .open<ConfirmDialog, Confirmation, boolean>(ConfirmDialog, {
        data: confirmation,
        role: 'alertdialog',
        autoFocus: 'dialog',
      })
      .afterClosed()
      .pipe(map((answer) => answer === true));
  }
}
