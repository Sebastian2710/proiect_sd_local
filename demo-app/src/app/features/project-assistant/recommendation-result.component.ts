import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { finalize } from 'rxjs';

import { ProjectAssistantService } from '../../services/project-assistant.service';
import {
  AvailabilityStatus,
  RecommendationSession,
} from '../../models/recommendation.model';
import { MarkdownPipe } from '../../shared/markdown.pipe';

interface EditableItem {
  recommendedItemId: string;
  name: string;
  reason: string;
  availabilityStatus: AvailabilityStatus | null;
  quantity: number;
  selected: boolean;
  /** True when the validator chain couldn't resolve this LLM name to a catalog entry. */
  isUnmatched: boolean;
}

@Component({
  selector: 'app-recommendation-result',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    MarkdownPipe,
  ],
  templateUrl: './recommendation-result.component.html',
  styleUrl: './recommendation-result.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecommendationResultComponent {
  private readonly assistantService = inject(ProjectAssistantService);
  private readonly destroyRef = inject(DestroyRef);

  readonly session = input.required<RecommendationSession>();
  readonly submitted = output<void>();
  readonly resetRequested = output<void>();

  protected readonly editableItems = signal<EditableItem[]>([]);
  protected readonly isSubmitting = signal(false);
  protected readonly submitError = signal<string | null>(null);
  protected readonly displayedColumns = [
    'select',
    'name',
    'quantity',
    'reason',
    'availability',
  ];

  constructor() {
    // Whenever the session input arrives or changes, derive editable items.
    // Using effect() here (rather than computed()) because we need a writable
    // signal to support quantity edits and selection toggling.
    effect(() => {
      const session = this.session();
      this.editableItems.set(
        session.items.map((item) => ({
          recommendedItemId: item.id,
          name: item.equipment?.name ?? `Unmatched: ${item.originalLlmName}`,
          reason: item.reason,
          availabilityStatus: item.availabilityStatus,
          quantity: item.quantity,
          // Default: include every resolved item; auto-exclude unmatched ones since
          // they can't be submitted as a loan request anyway.
          selected: item.equipment !== null,
          isUnmatched: item.equipment === null,
        })),
      );
    });
  }

  protected toggleSelected(item: EditableItem): void {
    if (item.isUnmatched) return;
    this.editableItems.update((items) =>
      items.map((i) =>
        i.recommendedItemId === item.recommendedItemId
          ? { ...i, selected: !i.selected }
          : i,
      ),
    );
  }

  protected updateQuantity(item: EditableItem, value: number): void {
    if (Number.isNaN(value) || value < 1) return;
    this.editableItems.update((items) =>
      items.map((i) =>
        i.recommendedItemId === item.recommendedItemId ? { ...i, quantity: value } : i,
      ),
    );
  }

  protected formatStatus(status: AvailabilityStatus | null, isUnmatched: boolean): string {
    if (isUnmatched) return 'UNMATCHED';
    if (!status) return '—';
    return status.replace('_', ' ');
  }

  protected statusClass(status: AvailabilityStatus | null, isUnmatched: boolean): string {
    if (isUnmatched) return 'availability availability-unmatched';
    if (!status) return 'availability';
    return `availability availability-${status.toLowerCase()}`;
  }

  protected submit(): void {
    if (this.isSubmitting()) return;

    const items = this.editableItems()
      .filter((i) => i.selected && !i.isUnmatched)
      .map((i) => ({ recommendedItemId: i.recommendedItemId, quantity: i.quantity }));

    if (items.length === 0) {
      this.submitError.set('Select at least one item to submit.');
      return;
    }

    this.submitError.set(null);
    this.isSubmitting.set(true);

    this.assistantService
      .submit(this.session().id, { items, expectedReturnDate: null })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => this.submitted.emit(),
        error: (err: HttpErrorResponse) => this.submitError.set(this.extractError(err)),
      });
  }

  protected reset(): void {
    this.resetRequested.emit();
  }

  private extractError(err: HttpErrorResponse): string {
    const body = err.error as Record<string, string> | null;
    if (body?.['business_error']) return body['business_error'];
    if (err.status === 0) return 'Could not reach the server.';
    return 'Could not submit. Please try again.';
  }
}
