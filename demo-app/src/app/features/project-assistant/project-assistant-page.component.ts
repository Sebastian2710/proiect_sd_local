import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatToolbar } from '@angular/material/toolbar';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { LoginStore } from '../login/login.store';
import { ProjectAssistantService } from '../../services/project-assistant.service';
import { RecommendationSession } from '../../models/recommendation.model';
import { RecommendationResultComponent } from './recommendation-result.component';

@Component({
  selector: 'app-project-assistant-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatNativeDateModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatToolbar,
    RouterLink,
    RecommendationResultComponent,
  ],
  templateUrl: './project-assistant-page.component.html',
  styleUrl: './project-assistant-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectAssistantPageComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly assistantService = inject(ProjectAssistantService);
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly form = this.fb.group({
    description: [
      '',
      [Validators.required, Validators.minLength(10), Validators.maxLength(5000)],
    ],
    expectedReturnDate: [null as Date | null, [Validators.required]],
  });

  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly session = signal<RecommendationSession | null>(null);

  /** Datepicker won't allow anything before tomorrow (matches @Future on the backend). */
  protected readonly minDate = new Date(Date.now() + 24 * 60 * 60 * 1000);

  protected submit(): void {
    if (this.isLoading()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { description, expectedReturnDate } = this.form.getRawValue();
    if (!expectedReturnDate) return;

    this.errorMessage.set(null);
    this.isLoading.set(true);

    this.assistantService
      .recommend({
        description: description.trim(),
        expectedReturnDate: this.toIsoDate(expectedReturnDate),
      })
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (session) => this.session.set(session),
        error: (err: HttpErrorResponse) => this.errorMessage.set(this.extractError(err)),
      });
  }

  protected onSessionSubmitted(): void {
    void this.router.navigate(['/student']);
  }

  protected resetForm(): void {
    this.session.set(null);
    this.form.reset({ description: '', expectedReturnDate: null });
    this.errorMessage.set(null);
  }

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }

  /** Local-date YYYY-MM-DD; avoids the UTC drift you get from {@code toISOString}. */
  private toIsoDate(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private extractError(err: HttpErrorResponse): string {
    const body = err.error as Record<string, string> | null;
    if (body?.['business_error']) return body['business_error'];
    if (body?.['description']) return body['description'];
    if (body?.['expectedReturnDate']) return body['expectedReturnDate'];
    if (err.status === 0) return 'Could not reach the server.';
    return 'Something went wrong. Please try again.';
  }
}
