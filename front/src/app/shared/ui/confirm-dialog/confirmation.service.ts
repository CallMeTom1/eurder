import { inject, Service } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { map, Observable } from 'rxjs';
import { Confirmation, ConfirmDialog, DialogData, Information } from './confirm-dialog';

@Service()
export class ConfirmationService {
  private readonly dialog = inject(MatDialog);

  confirm(confirmation: Confirmation): Observable<boolean> {
    return this.dialog
      .open<ConfirmDialog, DialogData, boolean>(ConfirmDialog, {
        data: confirmation,
        role: 'alertdialog',
        autoFocus: 'dialog',
      })
      .afterClosed()
      .pipe(map((answer) => answer === true));
  }

  inform(information: Information): Observable<void> {
    return this.dialog
      .open<ConfirmDialog, DialogData, boolean>(ConfirmDialog, {
        data: { ...information, informOnly: true },
        autoFocus: 'dialog',
      })
      .afterClosed()
      .pipe(map(() => undefined));
  }
}
