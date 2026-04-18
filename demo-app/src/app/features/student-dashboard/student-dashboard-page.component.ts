import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbar } from '@angular/material/toolbar';
import { Router } from '@angular/router';
import { LoginStore } from '../login/login.store';

@Component({
  selector: 'app-student-dashboard-page',
  imports: [MatToolbar, MatButtonModule, MatIconModule],
  templateUrl: './student-dashboard-page.component.html',
  styleUrl: './student-dashboard-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentDashboardPageComponent {
  private readonly loginStore = inject(LoginStore);
  private readonly router = inject(Router);

  protected logout(): void {
    this.loginStore.logout();
    void this.router.navigate(['/login']);
  }
}
