package com.gudosnest.backend.service;

import com.gudosnest.backend.database.Db;
import com.gudosnest.backend.repository.ReportRepository;
import com.gudosnest.backend.repository.ReportRepository.Query;

import java.util.List;

/** The owner's report queries. Read-only: nothing here changes data. */
public class ReportService {

    private final ReportRepository reports = new ReportRepository();

    public List<Query> queries() {
        return reports.queries();
    }

    /** One query by its number (1-8). */
    public Query query(int number) {
        return reports.queries().stream().filter(q -> q.number() == number).findFirst()
                .orElseThrow(() -> new RuleException("That report does not exist."));
    }

    public Db.Table run(Query query, int ownerId) {
        if (query == null) {
            throw new RuleException("Choose a report first.");
        }
        return reports.run(query, ownerId);
    }
}
