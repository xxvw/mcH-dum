package com.github.n9.mch.server;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "managed_servers")
public class ManagedServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(nullable = false)
    private int port;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ManagedServerStatus status = ManagedServerStatus.STOPPED;

    @Column(length = 32)
    private String source;

    @Column(length = 64)
    private String ownerDiscordId;

    @Column(length = 64)
    private String discordChannelId;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastStartedAt;
    private Instant lastStoppedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ManagedServerStatus getStatus() {
        return status;
    }

    public void setStatus(ManagedServerStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getOwnerDiscordId() {
        return ownerDiscordId;
    }

    public void setOwnerDiscordId(String ownerDiscordId) {
        this.ownerDiscordId = ownerDiscordId;
    }

    public String getDiscordChannelId() {
        return discordChannelId;
    }

    public void setDiscordChannelId(String discordChannelId) {
        this.discordChannelId = discordChannelId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastStartedAt() {
        return lastStartedAt;
    }

    public void setLastStartedAt(Instant lastStartedAt) {
        this.lastStartedAt = lastStartedAt;
    }

    public Instant getLastStoppedAt() {
        return lastStoppedAt;
    }

    public void setLastStoppedAt(Instant lastStoppedAt) {
        this.lastStoppedAt = lastStoppedAt;
    }
}
