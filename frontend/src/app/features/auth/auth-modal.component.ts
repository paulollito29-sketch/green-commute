import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-auth-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
      <div class="bg-slate-900 border border-white/10 rounded-3xl p-6 max-w-sm w-full shadow-2xl space-y-4 relative">
        <button (click)="close.emit()" class="absolute top-4 right-4 text-slate-400 hover:text-white">✕</button>
        <div class="text-center space-y-1">
          <div class="w-10 h-10 bg-emerald-500/20 text-emerald-400 rounded-2xl flex items-center justify-center mx-auto text-xl mb-2">🌱</div>
          <h3 class="text-base font-extrabold text-white">{{ isRegister ? 'Crear Cuenta' : 'Ingresar a EcoCommute' }}</h3>
        </div>

        <form (ngSubmit)="submit()" class="space-y-3">
          <div *ngIf="isRegister">
            <label class="text-[10px] font-bold text-slate-400 uppercase">Nombre Completo</label>
            <input [(ngModel)]="fullName" name="fullName" required placeholder="Carlos Silva" class="w-full bg-slate-950 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none mt-1" />
          </div>
          <div>
            <label class="text-[10px] font-bold text-slate-400 uppercase">Correo</label>
            <input [(ngModel)]="email" name="email" type="email" required placeholder="correo@ejemplo.com" class="w-full bg-slate-950 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none mt-1" />
          </div>
          <div>
            <label class="text-[10px] font-bold text-slate-400 uppercase">Contraseña</label>
            <input [(ngModel)]="password" name="password" type="password" required placeholder="••••••••" class="w-full bg-slate-950 border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none mt-1" />
          </div>
          <button type="submit" class="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-2.5 rounded-xl text-xs transition">
            {{ isRegister ? 'Registrarse' : 'Iniciar Sesión' }}
          </button>
        </form>

        <div class="text-center text-xs text-slate-400">
          <button (click)="isRegister = !isRegister" class="text-emerald-400 font-bold hover:underline">
            {{ isRegister ? '¿Ya tienes cuenta? Inicia sesión' : '¿No tienes cuenta? Crear cuenta' }}
          </button>
        </div>
      </div>
    </div>
  `
})
export class AuthModalComponent {
  isRegister = false;
  fullName = '';
  email = '';
  password = '';

  @Output() close = new EventEmitter<void>();

  constructor(private authService: AuthService) {}

  submit(): void {
    if (this.isRegister) {
      this.authService.register(this.fullName, this.email, this.password).subscribe(() => this.close.emit());
    } else {
      this.authService.login(this.email, this.password).subscribe(() => this.close.emit());
    }
  }
}
