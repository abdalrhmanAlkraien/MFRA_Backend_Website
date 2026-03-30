export interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
  error?: ApiError;
}

export interface ApiError {
  code: string;
  message: string;
  fields?: Record<string, string>;
}

export interface UserInfo {
  id: string;
  fullName: string;
  email: string;
  role: 'ADMIN' | 'EDITOR';
  avatarUrl: string | null;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}
