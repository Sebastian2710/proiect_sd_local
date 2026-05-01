import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Equipment } from '../../models/equipment.model';
import { EquipmentQuantityDto, StudentLoanRequestDto } from '../../models/loan-record.model';

export interface StudentLoanRequestDialogData {
  availableEquipment: Equipment[];
}

export type StudentLoanRequestDialogResult = StudentLoanRequestDto | undefined;

@Component({
  selector: 'app-student-loan-request-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './student-loan-request-dialog.component.html',
  styleUrl: './student-loan-request-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentLoanRequestDialogComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly dialogRef = inject(MatDialogRef<StudentLoanRequestDialogComponent>);

  protected readonly data = inject<StudentLoanRequestDialogData>(MAT_DIALOG_DATA);
  protected readonly today = new Date().toISOString().split('T')[0];

  protected readonly form = this.fb.group({
    expectedReturnDate: ['', [Validators.required]],
    items: this.fb.array([this.createItemGroup()]),
  });

  private createItemGroup() {
    return this.fb.group({
      equipmentId: ['', [Validators.required]],
      quantity: [1, [Validators.required, Validators.min(1)]],
    });
  }

  get itemControls() {
    return this.form.controls.items.controls;
  }

  protected addItem(): void {
    this.form.controls.items.push(this.createItemGroup());
  }

  protected removeItem(index: number): void {
    if (this.form.controls.items.length > 1) {
      this.form.controls.items.removeAt(index);
    }
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { expectedReturnDate, items } = this.form.getRawValue();
    const equipmentQuantities: EquipmentQuantityDto[] = items.map((i) => ({
      equipmentId: i.equipmentId,
      quantity: i.quantity,
    }));
    this.dialogRef.close({ equipmentQuantities, expectedReturnDate } as StudentLoanRequestDto);
  }

  protected cancel(): void {
    this.dialogRef.close(undefined);
  }
}
