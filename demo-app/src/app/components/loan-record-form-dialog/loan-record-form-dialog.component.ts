import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Equipment } from '../../models/equipment.model';
import {
  EquipmentQuantityDto,
  LoanRecordCreateDto,
  LoanRecordUpdateDto,
} from '../../models/loan-record.model';
import { Person } from '../../models/person.model';
import { EquipmentService } from '../../services/equipment.service';
import { PersonService } from '../../services/person.service';

export type LoanFormMode = 'create' | 'update';

export interface LoanRecordFormDialogData {
  mode: LoanFormMode;
  initialValue?: {
    status?: string;
    expectedReturnDate?: string;
    actualReturnDate?: string | null;
  } | null;
}

export type LoanRecordFormDialogResult =
  | LoanRecordCreateDto
  | LoanRecordUpdateDto
  | undefined;

const LOAN_STATUSES = ['PROCESSING', 'ACTIVE', 'RETURNED', 'OVERDUE'] as const;

@Component({
  selector: 'app-loan-record-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './loan-record-form-dialog.component.html',
  styleUrl: './loan-record-form-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoanRecordFormDialogComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly dialogRef = inject(MatDialogRef<LoanRecordFormDialogComponent>);
  private readonly personService = inject(PersonService);
  private readonly equipmentService = inject(EquipmentService);

  protected readonly data = inject<LoanRecordFormDialogData>(MAT_DIALOG_DATA);
  protected readonly statuses = LOAN_STATUSES;
  protected readonly persons = signal<Person[]>([]);
  protected readonly equipment = signal<Equipment[]>([]);
  protected readonly loadingRefs = signal(false);
  protected readonly today = new Date().toISOString().split('T')[0];

  // Create form with items FormArray
  protected readonly createForm = this.fb.group({
    personId: ['', [Validators.required]],
    expectedReturnDate: ['', [Validators.required]],
    items: this.fb.array([this.createItemGroup()]),
  });

  // Update form
  protected readonly updateForm = this.fb.group({
    status: ['', [Validators.required]],
    expectedReturnDate: [''],
    actualReturnDate: [''],
  });

  private createItemGroup() {
    return this.fb.group({
      equipmentId: ['', [Validators.required]],
      quantity: [1, [Validators.required, Validators.min(1)]],
    });
  }

  get itemControls() {
    return this.createForm.controls.items.controls;
  }

  protected addItem(): void {
    this.createForm.controls.items.push(this.createItemGroup());
  }

  protected removeItem(index: number): void {
    if (this.createForm.controls.items.length > 1) {
      this.createForm.controls.items.removeAt(index);
    }
  }

  ngOnInit(): void {
    if (this.data.mode === 'create') {
      this.loadRefs();
    } else if (this.data.initialValue) {
      this.updateForm.patchValue({
        status: this.data.initialValue.status ?? '',
        expectedReturnDate: this.data.initialValue.expectedReturnDate ?? '',
        actualReturnDate: this.data.initialValue.actualReturnDate ?? '',
      });
    }
  }

  private loadRefs(): void {
    this.loadingRefs.set(true);
    let done = 0;
    const check = () => {
      done++;
      if (done === 2) this.loadingRefs.set(false);
    };
    this.personService.getAll().subscribe({
      next: (p) => { this.persons.set(p); check(); },
      error: () => check(),
    });
    this.equipmentService.getAll().subscribe({
      next: (e) => { this.equipment.set(e); check(); },
      error: () => check(),
    });
  }

  protected submit(): void {
    if (this.data.mode === 'create') {
      if (this.createForm.invalid) {
        this.createForm.markAllAsTouched();
        return;
      }
      const { personId, expectedReturnDate, items } = this.createForm.getRawValue();
      const equipmentQuantities: EquipmentQuantityDto[] = items.map((i) => ({
        equipmentId: i.equipmentId,
        quantity: i.quantity,
      }));
      this.dialogRef.close({ personId, equipmentQuantities, expectedReturnDate } as LoanRecordCreateDto);
    } else {
      if (this.updateForm.invalid) {
        this.updateForm.markAllAsTouched();
        return;
      }
      const { status, expectedReturnDate, actualReturnDate } = this.updateForm.getRawValue();
      const dto: LoanRecordUpdateDto = { status };
      if (expectedReturnDate) dto.expectedReturnDate = expectedReturnDate;
      if (actualReturnDate) dto.actualReturnDate = actualReturnDate;
      this.dialogRef.close(dto);
    }
  }

  protected cancel(): void {
    this.dialogRef.close(undefined);
  }
}
