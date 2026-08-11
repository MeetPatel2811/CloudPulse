package com.cloudpulse.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

// One entry on an incident's activity timeline: an acknowledgement, an owner
// assignment, a free-text note, or a resolution. Entries are read in creation
// order so the timeline flows oldest to newest.
@Entity
@Table(name = "incident_activity")
public class IncidentActivity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private Alert alert;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentActivityType type;

    // Who performed the action (e.g. the operator's name); may be null.
    @Column
    private String author;

    // The note text, or a short description of the lifecycle action.
    @Column(length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected IncidentActivity() {
    }

    public IncidentActivity(Alert alert, IncidentActivityType type, String author, String detail) {
        this.alert = alert;
        this.type = type;
        this.author = author;
        this.detail = detail;
    }

    public UUID getId() { return id; }
    public Alert getAlert() { return alert; }
    public IncidentActivityType getType() { return type; }
    public String getAuthor() { return author; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
