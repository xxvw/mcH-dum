package com.github.n9.mch.server;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagedServerRepository extends JpaRepository<ManagedServer, Long> {

    Optional<ManagedServer> findByName(String name);

    boolean existsByName(String name);
}
