package com.licenta.horeca.controller;

import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.service.TableSessionService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@RestController
@RequestMapping("/api/table-sessions")
@CrossOrigin(origins = "http://localhost:5173")
public class TableSessionController {

    private final TableSessionService tableSessionService;

    public TableSessionController(TableSessionService tableSessionService) {
        this.tableSessionService = tableSessionService;
    }

    @GetMapping("/active")
    public List<TableSession> getActiveSessions() {
        return tableSessionService.getActiveSessions();
    }

    @PostMapping("/table/{tableId}")
    public TableSession createSessionForTable(@PathVariable Long tableId) {
        return tableSessionService.createSessionForTable(tableId);
    }
}