import { Injectable, signal, computed } from '@angular/core';
import { ApiService } from './api.service';
import { User, AuthResponse } from '../models/user.model';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSignal = signal<User | null>(null);
  public currentUser = this.currentUserSignal.asReadonly();
  public isAuthenticated = computed(() => !!this.currentUserSignal());
  public isAdmin = computed(() => this.currentUserSignal()?.role === 'ROLE_ADMIN');

  constructor(private api: ApiService) {
    this.restoreSession();
  }

  private restoreSession(): void {
    const savedUser = localStorage.getItem('ecocommute_user');
    const token = localStorage.getItem('ecocommute_token');
    if (savedUser && token) {
      try {
        this.currentUserSignal.set(JSON.parse(savedUser));
      } catch (e) {
        this.logout();
      }
    }
  }

  login(email: string, password: string):Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/login', { email, password }).pipe(
      tap(res => this.handleAuthSuccess(res))
    );
  }

  register(fullName: string, email: string, password: string): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/register', { fullName, email, password }).pipe(
      tap(res => this.handleAuthSuccess(res))
    );
  }

  private handleAuthSuccess(res: AuthResponse): void {
    localStorage.setItem('ecocommute_token', res.token);
    const user: User = {
      id: res.id,
      email: res.email,
      fullName: res.fullName,
      role: res.role,
      active: true,
      points: res.points || 0
    };
    localStorage.setItem('ecocommute_user', JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  logout(): void {
    localStorage.removeItem('ecocommute_token');
    localStorage.removeItem('ecocommute_user');
    this.currentUserSignal.set(null);
  }
}
