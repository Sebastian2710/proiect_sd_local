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
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PersonService } from '../../services/person.service';
import { EquipmentService } from '../../services/equipment.service';
import { Person } from '../../models/person.model';
import { Equipment } from '../../models/equipment.model';
import { LoanRecordCreateDto, LoanRecordUpdateDto } from '../../models/loan-record.model';

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

const LOAN_STATUSES = ['ACTIVE', 'RETURNED', 'OVERDUE'] as const;

@Component({
  selector: 'app-loan-record-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
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

  protected readonly createForm = this.fb.group({
    personId: ['', [Validators.required]],
    equipmentIds: [[] as string[], [Validators.required, Validators.minLength(1)]],
    expectedReturnDate: ['', [Validators.required]],
  });

  protected readonly updateForm = this.fb.group({
    status: ['', [Validators.required]],
    expectedReturnDate: [''],
    actualReturnDate: [''],
  });

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
    this.personService.getAll().subscribe({ next: (p) => { this.persons.set(p); check(); }, error: () => check() });
    this.equipmentService.getAll().subscribe({ next: (e) => { this.equipment.set(e); check(); }, error: () => check() });
  }

  protected submit(): void {
    if (this.data.mode === 'create') {
      if (this.createForm.invalid) {
        this.createForm.markAllAsTouched();
        return;
      }
      const { personId, equipmentIds, expectedReturnDate } = this.createForm.getRawValue();
      this.dialogRef.close({ personId, equipmentIds, expectedReturnDate } as LoanRecordCreateDto);
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
