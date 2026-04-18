import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface EquipmentFormDialogData {
  title: string;
  submitLabel?: string;
  initialValue?: { name: string; description: string; stockCount: number } | null;
}

export interface EquipmentFormValue {
  name: string;
  description: string;
  stockCount: number;
}

export type EquipmentFormDialogResult = EquipmentFormValue | undefined;

@Component({
  selector: 'app-equipment-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './equipment-form-dialog.component.html',
  styleUrl: './equipment-form-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EquipmentFormDialogComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly dialogRef = inject(MatDialogRef<EquipmentFormDialogComponent>);
  protected readonly data = inject<EquipmentFormDialogData>(MAT_DIALOG_DATA);

  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    description: ['', [Validators.required]],
    stockCount: [0, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    if (this.data.initialValue) {
      this.form.patchValue(this.data.initialValue);
    }
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.getRawValue());
  }

  protected cancel(): void {
    this.dialogRef.close(undefined);
  }
}
