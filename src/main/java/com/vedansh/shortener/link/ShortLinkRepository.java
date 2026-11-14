package com.vedansh.shortener.link;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    Optional<ShortLink> findByCode(String code);

    boolean existsByCode(String code);

    List<ShortLink> findTop100ByOrderByCreatedAtDesc();

    /** Atomic increment, so concurrent clicks are never lost. */
    @Modifying(clearAutomatically = true)
    @Query("update ShortLink l set l.clicks = l.clicks + 1, l.lastAccessedAt = :now where l.id = :id")
    int registerClick(@Param("id") Long id, @Param("now") Instant now);
}
