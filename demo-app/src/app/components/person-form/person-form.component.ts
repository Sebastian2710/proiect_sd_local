import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CreatePersonDto, Person, UpdatePersonDto } from '../../models/person.model';

export interface PersonFormDialogData {
  person?: Person;
}

export type PersonFormDialogResult = CreatePersonDto | UpdatePersonDto | undefined;

@Component({
  selector: 'app-person-form',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './person-form.component.html',
  styleUrl: './person-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PersonFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<PersonFormComponent>);
  protected readonly data = inject<PersonFormDialogData>(MAT_DIALOG_DATA);

  protected readonly isEditMode = signal(false);
  protected readonly isPasswordVisible = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    age: [0, [Validators.required, Validators.min(0), Validators.max(1000)]],
    email: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    if (this.data?.person) {
      this.isEditMode.set(true);
      const { name, age, email } = this.data.person;
      this.form.patchValue({ name, age, email });
      this.form.controls.password.clearValidators();
      this.form.controls.password.updateValueAndValidity();
    }
  }

  protected togglePasswordVisibility(): void {
    this.isPasswordVisible.update((visible) => !visible);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, age, email, password } = this.form.getRawValue();

    if (this.isEditMode()) {
      const result: UpdatePersonDto = { name, age, email, password: this.data.person!.password };
      this.dialogRef.close(result);
    } else {
      const result: CreatePersonDto = { name, age, email, password };
      this.dialogRef.close(result);
    }
  }

  protected cancel(): void {
    this.dialogRef.close(undefined);
  }
}
