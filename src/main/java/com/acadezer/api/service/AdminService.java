package com.acadezer.api.service;

import com.acadezer.api.dto.response.AdminStatsResponse;
import com.acadezer.api.dto.response.UserResponse;
import com.acadezer.api.enums.UserRole;
import com.acadezer.api.exception.ApiException;
import com.acadezer.api.mapper.UserMapper;
import com.acadezer.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final UserRepository users;
    private final UserMapper mapper;
    private final SubjectRepository subjects;
    private final NoteRepository notes;
    private final TaskRepository tasks;
    private final ExamRepository exams;
    private final AttendanceRepository attendance;
    private final AttendanceControlRepository controls;

    public List<UserResponse> listUsers() {
        return users.findAll(Sort.by("id")).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        var user = users.findById(id).orElseThrow(ApiException::notFound);
        if (user.getRole() == UserRole.ADMIN) {
            throw new ApiException(409, "Este endpoint administra únicamente cuentas de estudiantes");
        }
        user.setEnabled(enabled);
        return mapper.toResponse(user);
    }

    public AdminStatsResponse stats() {
        return new AdminStatsResponse(users.count(), users.countByEnabledTrue(),
            users.countByRole(UserRole.STUDENT), users.countByRole(UserRole.ADMIN),
            subjects.count(), notes.count(), tasks.count(), exams.count(), attendance.count() + controls.count());
    }
}
