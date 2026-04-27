import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatStepperModule } from '@angular/material/stepper';
import { RouterLink } from '@angular/router';
import { PasswordResetService } from '../../services/password-reset.service';

type Step = 'request' | 'reset' | 'done';

@Component({
  selector: 'app-forgot-password',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatStepperModule,
    RouterLink,
  ],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForgotPasswordComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly passwordResetService = inject(PasswordResetService);

  protected readonly step = signal<Step>('request');
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly isPasswordVisible = signal(false);
  protected readonly isConfirmPasswordVisible = signal(false);

  protected readonly emailForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  protected readonly resetForm = this.fb.group({
    code: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
  });

  protected get storedEmail(): string {
    return this.emailForm.getRawValue().email;
  }

  protected togglePasswordVisibility(): void {
    this.isPasswordVisible.update((v) => !v);
  }

  protected toggleConfirmPasswordVisibility(): void {
    this.isConfirmPasswordVisible.update((v) => !v);
  }

  protected submitEmail(): void {
    if (this.emailForm.invalid || this.isSubmitting()) {
      this.emailForm.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);
    this.isSubmitting.set(true);

    this.passwordResetService
      .requestReset({ email: this.storedEmail })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.step.set('reset');
        },
        error: (err: { error?: Record<string, string> }) => {
          this.isSubmitting.set(false);
          const errorData = err.error ?? {};
          this.errorMessage.set(
            errorData['business_error'] ||
            Object.values(errorData)[0] ||
            'Failed to send reset code. Please try again.',
          );
        },
      });
  }

  protected submitReset(): void {
    if (this.resetForm.invalid || this.isSubmitting()) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const { newPassword, confirmPassword, code } = this.resetForm.getRawValue();

    if (newPassword !== confirmPassword) {
      this.errorMessage.set('Passwords do not match.');
      return;
    }

    this.errorMessage.set(null);
    this.isSubmitting.set(true);

    this.passwordResetService
      .resetPassword({ email: this.storedEmail, code, newPassword })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.step.set('done');
        },
        error: (err: { error?: Record<string, string> }) => {
          this.isSubmitting.set(false);
          const errorData = err.error ?? {};
          this.errorMessage.set(
            errorData['business_error'] ||
            Object.values(errorData)[0] ||
            'Failed to reset password. Please try again.',
          );
        },
      });
  }
}
