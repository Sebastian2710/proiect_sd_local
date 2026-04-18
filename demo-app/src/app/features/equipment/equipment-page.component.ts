import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatToolbar } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import {
  ConfirmDeleteDialogComponent,
  ConfirmDeleteDialogData,
} from '../../components/confirm-delete-dialog/confirm-delete-dialog.component';
import {
  EquipmentFormDialogComponent,
  EquipmentFormDialogData,
  EquipmentFormDialogResult,
  EquipmentFormValue,
} from '../../components/equipment-form-dialog/equipment-form-dialog.component';
import { Equipment } from '../../models/equipment.model';
import { LoginStore } from '../login/login.store';
import { EquipmentStore, SortOption, StockFilter } from './equipment.store';

@Component({
  selector: 'app-equipment-page',
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
    RouterLinkActive,
  ],
  templateUrl: './equipment-page.component.html',
  styleUrl: './equipment-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EquipmentPageComponent {
  private readonly dialog = inject(MatDialog);
  private readonly store = inject(EquipmentStore);
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly filteredEquipment = this.store.filteredEquipment;
  protected readonly hasError = this.store.hasError;
  protected readonly errorMsg = this.store.errorMsg;
  protected readonly isLoading = this.store.isLoading;
  protected readonly searchQuery = this.store.searchQuery;
  protected readonly stockFilter = this.store.stockFilter;
  protected readonly sortOption = this.store.sortOption;
  protected readonly displayedColumns = ['name', 'description', 'stockCount', 'actions'];

  constructor() {
    this.store.load();
  }

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }

  protected onSearchChange(value: string): void {
    this.store.searchQuery.set(value);
  }

  protected onStockFilterChange(value: StockFilter): void {
    this.store.stockFilter.set(value);
  }

  protected onSortChange(value: SortOption): void {
    this.store.sortOption.set(value);
  }

  protected openCreateDialog(): void {
    if (this.isLoading()) return;

    this.dialog
      .open<EquipmentFormDialogComponent, EquipmentFormDialogData, EquipmentFormDialogResult>(
        EquipmentFormDialogComponent,
        { data: { title: 'Add Equipment', submitLabel: 'Create' } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        this.store.create(result as EquipmentFormValue);
      });
  }

  protected openEditDialog(equipment: Equipment): void {
    if (this.isLoading()) return;

    this.dialog
      .open<EquipmentFormDialogComponent, EquipmentFormDialogData, EquipmentFormDialogResult>(
        EquipmentFormDialogComponent,
        {
          data: {
            title: 'Edit Equipment',
            submitLabel: 'Save',
            initialValue: equipment,
          },
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (!result) return;
        this.store.update(equipment.id, result as EquipmentFormValue);
      });
  }

  protected openDeleteDialog(equipment: Equipment): void {
    if (this.isLoading()) return;

    this.dialog
      .open<ConfirmDeleteDialogComponent, ConfirmDeleteDialogData, boolean>(
        ConfirmDeleteDialogComponent,
        { data: { name: equipment.name } },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.store.remove(equipment.id);
      });
  }
}
