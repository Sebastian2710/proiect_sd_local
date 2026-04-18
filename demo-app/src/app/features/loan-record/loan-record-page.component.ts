import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatToolbar } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import {
  ConfirmDeleteDialogComponent,
  ConfirmDeleteDialogData,
} from '../../components/confirm-delete-dialog/confirm-delete-dialog.component';
import {
  LoanRecordFormDialogComponent,
  LoanRecordFormDialogData,
  LoanRecordFormDialogResult,
} from '../../components/loan-record-form-dialog/loan-record-form-dialog.component';
import { LoanRecord, LoanRecordCreateDto, LoanRecordUpdateDto } from '../../models/loan-record.model';
import { LoginStore } from '../login/login.store';
import { LoanRecordStore } from './loan-record.store';

@Component({
  selector: 'app-loan-record-page',
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatToolbar,
    RouterLink,
    RouterLinkActive,
  ],
  templateUrl: './loan-record-page.component.html',
  styleUrl: './loan-record-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoanRecordPageComponent {
  private readonly dialog = inject(MatDialog);
  private readonly store = inject(LoanRecordStore);
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loanRecords = this.store.loanRecords;
  protected readonly hasError = this.store.hasError;
  protected readonly errorMsg = this.store.errorMsg;
  protected readonly isLoading = this.store.isLoading;
  protected readonly displayedColumns = [
    'person',
    'loanDate',
    'expectedReturnDate',
    'actualReturnDate',
    'status',
    'equipment',
    'actions',
  ];

  constructor() {
    this.store.load();
  }

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }

  protected formatEquipment(record: LoanRecord): string {
    return record.equipmentList.map((e) => e.name).join(', ') || '—';
  }

  protected openCreateDialog(): void {
    if (this.isLoading()) return;

    this.dialog
      .open<LoanRecordFormDialogComponent, LoanRecordFormDialogData, LoanRecordFormDialogResult>(
        LoanRecordFormDialogComponent,
        { data: { mode: 'create' } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        this.store.create(result as LoanRecordCreateDto);
      });
  }

  protected openEditDialog(record: LoanRecord): void {
    if (this.isLoading()) return;

    this.dialog
      .open<LoanRecordFormDialogComponent, LoanRecordFormDialogData, LoanRecordFormDialogResult>(
        LoanRecordFormDialogComponent,
        {
          data: {
            mode: 'update',
            initialValue: {
              status: record.status,
              expectedReturnDate: record.expectedReturnDate,
              actualReturnDate: record.actualReturnDate,
            },
          },
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        this.store.update(record.id, result as LoanRecordUpdateDto);
      });
  }

  protected openDeleteDialog(record: LoanRecord): void {
    if (this.isLoading()) return;

    this.dialog
      .open<ConfirmDeleteDialogComponent, ConfirmDeleteDialogData, boolean>(
        ConfirmDeleteDialogComponent,
        { data: { name: `loan for ${record.person.name}` } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.store.remove(record.id);
      });
  }
}
