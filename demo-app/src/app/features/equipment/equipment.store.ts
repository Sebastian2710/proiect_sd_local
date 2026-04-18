import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { CreateEquipmentDto, Equipment, UpdateEquipmentDto } from '../../models/equipment.model';
import { EquipmentService } from '../../services/equipment.service';

export type StockFilter = 'all' | 'in-stock' | 'out-of-stock';
export type SortOption = 'name_asc' | 'name_desc' | 'stock_asc' | 'stock_desc';

@Injectable({ providedIn: 'root' })
export class EquipmentStore {
  private readonly equipmentService = inject(EquipmentService);
  private readonly pendingRequests = signal(0);

  readonly allEquipment = signal<Equipment[]>([]);
  readonly hasError = signal(false);
  readonly errorMsg = signal<string>('');
  readonly isLoading = computed(() => this.pendingRequests() > 0);

  // Filter / sort signals
  readonly searchQuery = signal('');
  readonly stockFilter = signal<StockFilter>('all');
  readonly sortOption = signal<SortOption>('name_asc');

  readonly filteredEquipment = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const stock = this.stockFilter();
    const sort = this.sortOption();

    let result = this.allEquipment();

    if (query) {
      result = result.filter((e) => e.name.toLowerCase().includes(query));
    }

    if (stock === 'in-stock') {
      result = result.filter((e) => e.stockCount > 0);
    } else if (stock === 'out-of-stock') {
      result = result.filter((e) => e.stockCount === 0);
    }

    result = [...result].sort((a, b) => {
      switch (sort) {
        case 'name_asc':
          return a.name.localeCompare(b.name);
        case 'name_desc':
          return b.name.localeCompare(a.name);
        case 'stock_asc':
          return a.stockCount - b.stockCount;
        case 'stock_desc':
          return b.stockCount - a.stockCount;
      }
    });

    return result;
  });

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
    this.equipmentService
      .getAll()
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (data) => this.allEquipment.set(data),
        error: (err: HttpErrorResponse) => this.handleError(err),
      });
  }

  create(dto: CreateEquipmentDto): void {
    this.hasError.set(false);
    this.beginRequest();
    this.equipmentService
      .create(dto)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (created) => this.allEquipment.update((list) => [...list, created]),
        error: (err: HttpErrorResponse) => this.handleError(err),
      });
  }

  update(id: string, dto: UpdateEquipmentDto): void {
    this.hasError.set(false);
    this.beginRequest();
    this.equipmentService
      .update(id, dto)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: (updated) =>
          this.allEquipment.update((list) =>
            list.map((e) => (e.id === updated.id ? updated : e)),
          ),
        error: (err: HttpErrorResponse) => this.handleError(err),
      });
  }

  remove(id: string): void {
    this.hasError.set(false);
    this.beginRequest();
    this.equipmentService
      .delete(id)
      .pipe(finalize(() => this.endRequest()))
      .subscribe({
        next: () =>
          this.allEquipment.update((list) => list.filter((e) => e.id !== id)),
        error: (err: HttpErrorResponse) => this.handleError(err),
      });
  }
}
