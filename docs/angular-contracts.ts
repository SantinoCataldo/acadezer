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
  tokenType: 'Bearer';
  expiresIn: number;
  usuario: UserResponse;
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
  fechaInicio: LocalDate;
  fechaFin: LocalDate;
  porcentajeAsistenciaMinima?: number | null;
  horarios: ScheduleRequest[];
}

export interface SubjectResponse {
  id: number;
  nombre: string;
  fechaInicio: LocalDate | null;
  fechaFin: LocalDate | null;
  porcentajeAsistenciaMinima: number;
  horarios: ScheduleResponse[];
}

export type Weekday = 'LUNES' | 'MARTES' | 'MIERCOLES' | 'JUEVES' | 'VIERNES' | 'SABADO' | 'DOMINGO';
export interface ScheduleRequest {
  id?: number | null;
  diaSemana: Weekday;
  horaInicio: string;
  horaFin: string;
  controlesAsistencia: 1 | 2;
}
export interface ScheduleResponse extends ScheduleRequest { id: number; }
export interface AttendanceControlRequest { estado: AttendanceStatus; }
export interface AttendanceControlResponse {
  id: number | null;
  horarioId: number;
  fecha: LocalDate;
  numero: number;
  horaInicio: string;
  horaFin: string;
  estado: AttendanceStatus | 'PENDIENTE';
}
export interface AttendanceSummaryResponse {
  presentes: number;
  ausentes: number;
  justificadas: number;
  pendientes: number;
  controlesMarcados: number;
  porcentajeAsistencia: number | null;
  porcentajeAsistenciaMinima: number;
  cumpleMinimo: boolean | null;
  controles: AttendanceControlResponse[];
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
