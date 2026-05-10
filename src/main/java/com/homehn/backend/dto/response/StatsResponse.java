package com.homehn.backend.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatsResponse {
    private long totalUsers;
    private long totalRooms;
    private long pendingRooms;
    private long totalReports;
}
