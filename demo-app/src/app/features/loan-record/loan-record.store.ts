import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { LoanRecord, LoanRecordCreateDto, LoanRecordUpdateDto } from '../../models/loan-record.model';
import { LoanRecordService } from '../../services/loan-record.service';

@Injectable({ providedIn: 'root' })
export class LoanRecordStore {
  private readonly loanRecordService = inject(LoanRecordService);
  private readonly pendingRequests = signal(0);

  readonly loanRecords = signal<LoanRecord[]>([]);
  readonly hasError = signal(false);
  readonly errorMsg = signal<string>('');
  readonly isLoading = computed(() => this.pendingRequests() > 0);

  private beginRequest(): void {
    this.pendingRequests.update((c) => c + 1);
  }

  private endRequest(): void {
    this.pendingRequests.update((c) => Math.max(0, c - 1));
  }

  private handleError(err: HttpErrorResponse): void {
    this.hasError.set(true);
    const errorData = err.error as Record<string, string>;
    const text =
      errorData?.['details'] ||
      Object.values(errorData || {})[0] ||
      'An unexpected error occurred!';
    this.errorMsg.set(text);
  }

  load(): void {
    this.hasError.set(false);
    this.beginRequest();
    this.loanRecordService
      .getAll()
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (data) => this.loanRecords.set(data),
        error: (err: HttpErrorResponse) => this.handleError(err),
      });
  }

  create(dto: LoanRecordCreateDto): void {
    this.hasError.set(false);
    this.beginRequest();
    this.loanRecordService
      .create(dto)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (created) => this.loanRecords.update((list) => [...list, created]),
        error: (err: HttpErrorResponse) => this.handleError(err),
      });
  }

  update(id: string, dto: LoanRecordUpdateDto): void {
    this.hasError.set(false);
    this.beginRequest();
    this.loanRecordService
      .update(id, dto)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (updated) =>
          this.loanRecords.update((list) =>
            list.map((r) => (r.id === updated.id ? updated : r)),
          ),
        error: (err: HttpErrorResponse) => this.handleError(err),
      });
  }

  remove(id: string): void {
    this.hasError.set(false);
    this.beginRequest();
    this.loanRecordService
      .delete(id)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: () =>
          this.loanRecords.update((list) => list.filter((r) => r.id !== id)),
        error: (err: HttpErrorResponse) => this.handleError(err),
      });
  }
}
