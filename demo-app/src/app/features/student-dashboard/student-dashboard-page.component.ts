import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatToolbar } from '@angular/material/toolbar';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  StudentLoanRequestDialogComponent,
  StudentLoanRequestDialogData,
  StudentLoanRequestDialogResult,
} from '../../components/student-loan-request-dialog/student-loan-request-dialog.component';
import { LoanRecord, StudentLoanRequestDto } from '../../models/loan-record.model';
import { LoanRecordService } from '../../services/loan-record.service';
import { LoginStore } from '../login/login.store';
import { EquipmentStore, SortOption, StockFilter } from '../equipment/equipment.store';

@Component({
  selector: 'app-student-dashboard-page',
  imports: [
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatToolbar,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    RouterLink,
  ],
  templateUrl: './student-dashboard-page.component.html',
  styleUrl: './student-dashboard-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentDashboardPageComponent {
  private readonly dialog = inject(MatDialog);
  private readonly equipmentStore = inject(EquipmentStore);
  private readonly loanRecordService = inject(LoanRecordService);
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // Equipment signals from store
  protected readonly filteredEquipment = this.equipmentStore.filteredEquipment;
  protected readonly isEquipmentLoading = this.equipmentStore.isLoading;
  protected readonly equipmentError = this.equipmentStore.hasError;
  protected readonly searchQuery = this.equipmentStore.searchQuery;
  protected readonly stockFilter = this.equipmentStore.stockFilter;
  protected readonly sortOption = this.equipmentStore.sortOption;

  // My loan records state
  protected readonly myLoanRecords = signal<LoanRecord[]>([]);
  protected readonly isLoansLoading = signal(false);
  protected readonly showMyLoans = signal(false);
  protected readonly loansError = signal<string | null>(null);

  // Request loan error
  protected readonly requestError = signal<string | null>(null);
  protected readonly requestSuccess = signal(false);

  protected readonly displayedEquipmentColumns = ['name', 'description', 'stockCount'];
  protected readonly displayedLoanColumns = [
    'equipment',
    'loanDate',
    'expectedReturnDate',
    'actualReturnDate',
    'status',
  ];

  constructor() {
    this.equipmentStore.load();
  }

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }

  protected onSearchChange(value: string): void {
    this.equipmentStore.searchQuery.set(value);
  }

  protected onStockFilterChange(value: StockFilter): void {
    this.equipmentStore.stockFilter.set(value);
  }

  protected onSortChange(value: SortOption): void {
    this.equipmentStore.sortOption.set(value);
  }

  protected toggleMyLoans(): void {
    const next = !this.showMyLoans();
    this.showMyLoans.set(next);
    if (next) {
      this.loadMyLoanRecords();
    }
  }

  private loadMyLoanRecords(): void {
    this.isLoansLoading.set(true);
    this.loansError.set(null);
    this.loanRecordService
      .getMyLoanRecords()
      .pipe(finalize(() => this.isLoansLoading.set(false)))
      .subscribe({
        next: (records) => this.myLoanRecords.set(records),
        error: () => this.loansError.set('Failed to load your loan records. Please try again.'),
      });
  }

  protected openRequestLoanDialog(): void {
    if (this.isEquipmentLoading()) return;

    this.requestError.set(null);
    this.requestSuccess.set(false);

    this.dialog
      .open<
    StudentLoanRequestDialogComponent,
      StudentLoanRequestDialogData,
    StudentLoanRequestDialogResult
    >(StudentLoanRequestDialogComponent, {
      data: { availableEquipment: this.equipmentStore.allEquipment() },
    })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        this.submitLoanRequest(result);
      });
  }

  private submitLoanRequest(dto: StudentLoanRequestDto): void {
    this.loanRecordService
      .createRequest(dto)
      .subscribe({
        next: () => {
          this.requestSuccess.set(true);
          this.equipmentStore.load();
          if (this.showMyLoans()) {
            this.loadMyLoanRecords();
          }
        },
        error: (err: HttpErrorResponse) => {
          const errorData = err.error as Record<string, string>;
          const msg =
            errorData?.['business_error'] ||
            Object.values(errorData || {})[0] ||
            'Failed to submit loan request. Please try again.';
          this.requestError.set(msg);
        },
      });
  }

  protected formatEquipment(record: LoanRecord): string {
    return record.items.map((item) => item.equipment.name).join(', ') || '—';
  }
}
