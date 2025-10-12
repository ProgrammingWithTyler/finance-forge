package com.financeforge.api.repository.mapper;

import com.financeforge.api.domain.model.User;
import com.financeforge.api.domain.model.enums.UserStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        return User.builder()
                .id(rs.getLong("ID"))
                .username(rs.getString("USERNAME"))
                .email(rs.getString("EMAIL"))
                .passwordHash(rs.getString("PASSWORD_HASH"))
                .firstName(rs.getString("FIRST_NAME"))
                .lastName(rs.getString("LAST_NAME"))
                .status(UserStatus.valueOf(rs.getString("STATUS")))
            .createdAt(rs.getTimestamp("CREATED_AT") != null
                ? rs.getTimestamp("CREATED_AT").toLocalDateTime()
                : null)
            .updatedAt(rs.getTimestamp("UPDATED_AT") != null
                ? rs.getTimestamp("UPDATED_AT").toLocalDateTime()
                : null)
                .createdBy(rs.getString("CREATED_BY"))
                .updatedBy(rs.getString("UPDATED_BY"))
                .build();
    }
}
