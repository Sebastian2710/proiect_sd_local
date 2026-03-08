import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ConfirmDeleteDialogComponent } from './components/confirm-delete-dialog/confirm-delete-dialog.component';
import {
  PersonFormComponent,
  PersonFormDialogData,
  PersonFormDialogResult,
} from './components/person-form/person-form.component';
import { CreatePersonDto, Person, UpdatePersonDto } from './models/person.model';
import { PersonService } from './services/person.service';

@Component({
  selector: 'app-root',
  imports: [
    MatToolbarModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App implements OnInit {
  private readonly personService = inject(PersonService);
  private readonly dialog = inject(MatDialog);

  protected readonly persons = signal<Person[]>([]);
  protected readonly hasError = signal(false);
  protected readonly displayedColumns = ['name', 'age', 'email', 'actions'];

  ngOnInit(): void {
    this.loadPersons();
  }

  private loadPersons(): void {
    this.hasError.set(false);
    this.personService.getAll().subscribe({
      next: (data) => this.persons.set(data),
      error: () => this.hasError.set(true),
    });
  }

  protected openCreateDialog(): void {
    const dialogRef = this.dialog.open<
      PersonFormComponent,
      PersonFormDialogData,
      PersonFormDialogResult
    >(PersonFormComponent, {
      width: '480px',
      data: {},
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (!result) return;
      this.hasError.set(false);
      this.personService.create(result as CreatePersonDto).subscribe({
        next: (created) => this.persons.update((list) => [...list, created]),
        error: () => this.hasError.set(true),
      });
    });
  }

  protected openEditDialog(person: Person): void {
    const dialogRef = this.dialog.open<
      PersonFormComponent,
      PersonFormDialogData,
      PersonFormDialogResult
    >(PersonFormComponent, {
      width: '480px',
      data: { person },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (!result) return;
      this.hasError.set(false);
      this.personService.update(person.id, result as UpdatePersonDto).subscribe({
        next: (updated) =>
          this.persons.update((list) =>
            list.map((p) => (p.id === updated.id ? updated : p)),
          ),
        error: () => this.hasError.set(true),
      });
    });
  }

  protected openDeleteDialog(person: Person): void {
    const dialogRef = this.dialog.open<ConfirmDeleteDialogComponent, { person: Person }, boolean>(
      ConfirmDeleteDialogComponent,
      { data: { person } },
    );

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.hasError.set(false);
      this.personService.delete(person.id).subscribe({
        next: () =>
          this.persons.update((list) => list.filter((p) => p.id !== person.id)),
        error: () => this.hasError.set(true),
      });
    });
  }
}
