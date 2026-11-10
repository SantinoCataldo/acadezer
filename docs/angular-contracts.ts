/** Matches the backend JSON contract. Java file/type names are in English. */
export type LocalDate = string; // YYYY-MM-DD

export enum AttendanceStatus {
  PRESENTE = 'PRESENTE',
  AUSENTE = 'AUSENTE',
  JUSTIFICADA = 'JUSTIFICADA',
}

export enum UserRole {
  STUDENT = 'STUDENT',
  ADMIN = 'ADMIN',
}

export interface RegisterRequest {
  nombre: string;
  email: string;
  password: string;
}

export interface AuthRequest {
  email: string;
  password: string;
}

export interface UserResponse {
  id: number;
  nombre: string;
  email: string;
  role: UserRole;
  enabled: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  usuario: UserResponse;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface UserEnabledRequest {
  enabled: boolean;
}

export interface AdminStatsResponse {
  usuarios: number;
  usuariosHabilitados: number;
  estudiantes: number;
  administradores: number;
  materias: number;
  apuntes: number;
  pendientes: number;
  evaluaciones: number;
  asistencias: number;
}

export interface SubjectRequest {
  nombre: string;
  descripcion?: string | null;
}

export interface SubjectResponse {
  id: number;
  nombre: string;
  descripcion: string | null;
}

export interface NoteRequest {
  titulo: string;
  contenido: string;
}

export interface NoteResponse extends NoteRequest {
  id: number;
  materiaId: number;
}

export interface TaskRequest {
  titulo: string;
  descripcion?: string | null;
  fechaLimite?: LocalDate | null;
}

export interface TaskCompletionRequest {
  completado: boolean;
}

export interface TaskResponse {
  id: number;
  titulo: string;
  descripcion: string | null;
  fechaLimite: LocalDate | null;
  completado: boolean;
  materiaId: number;
}

export interface ExamRequest {
  nombre: string;
  fecha: LocalDate;
  calificacion?: number | null;
}

export interface ExamResponse {
  id: number;
  nombre: string;
  fecha: LocalDate;
  calificacion: number | null;
  materiaId: number;
}

export interface AttendanceRequest {
  fecha: LocalDate;
  estado: AttendanceStatus;
}

export interface AttendanceResponse extends AttendanceRequest {
  id: number;
  materiaId: number;
}

export interface PracticeQuestionResponse {
  pregunta: string;
  respuesta: string;
}

export interface PracticeQuestionsResponse {
  preguntas: PracticeQuestionResponse[];
}

export interface ErrorResponse {
  status: number;
  message: string;
  path: string;
  timestamp: string; // ISO-8601 UTC
}
