export type Role = 'ROLE_USER' | 'ROLE_ADMIN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  avatarUrl?: string;
  role: Role;
  active: boolean;
  points: number;
}

export interface AuthResponse {
  token: string;
  id: string;
  email: string;
  fullName: string;
  role: Role;
  points: number;
}
