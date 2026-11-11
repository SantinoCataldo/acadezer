package com.acadezer.api;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

class ScheduleMigrationTests {
    @Test void migrationPreservesLegacySubjectsDescriptionsAndAttendance() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:migration_schedule;MODE=PostgreSQL", "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("create table usuarios (id bigint primary key)");
                statement.execute("create table materias (id bigint primary key, usuario_id bigint not null, descripcion varchar(2000))");
                statement.execute("create table asistencias (id bigint primary key, materia_id bigint not null, fecha date, estado varchar(15))");
                statement.execute("insert into usuarios values (1)");
                statement.execute("insert into materias values (1, 1, 'descripción anterior')");
                statement.execute("insert into asistencias values (1, 1, DATE '2026-01-01', 'PRESENTE')");
            }
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("database/add-schedules-and-controls.sql"));
            try (var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("select descripcion, fecha_inicio, fecha_fin, porcentaje_asistencia_minima from materias where id=1")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo("descripción anterior");
                    assertThat(result.getDate(2)).isNull(); assertThat(result.getDate(3)).isNull();
                    assertThat(result.getBigDecimal(4)).isEqualByComparingTo("75");
                }
                try (var result = statement.executeQuery("select estado from asistencias where id=1")) {
                    assertThat(result.next()).isTrue(); assertThat(result.getString(1)).isEqualTo("PRESENTE");
                }
                statement.execute("insert into horarios (materia_id, dia_semana, hora_inicio, hora_fin, controles_asistencia) values (1,'LUNES',TIME '08:00',TIME '12:00',2)");
                statement.execute("insert into controles_asistencia (horario_id, fecha, numero, estado) select id, DATE '2026-11-09', 1, 'PRESENTE' from horarios");
            }
            // Running the additive script again must not remove either legacy or new data.
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("database/add-schedules-and-controls.sql"));
            try (var statement = connection.createStatement(); var result = statement.executeQuery("select count(*) from controles_asistencia")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
    }
}
