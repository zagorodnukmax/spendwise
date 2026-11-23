package com.spendwise.transaction;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @EntityGraph(attributePaths = "category")
    List<Transaction> findByOwnerIdAndDateBetweenOrderByDateDescIdDesc(Long ownerId, LocalDate from, LocalDate to);

    Optional<Transaction> findByIdAndOwnerId(Long id, Long ownerId);

    @Modifying
    @Query("update Transaction t set t.category = null where t.category.id = :categoryId")
    void clearCategory(Long categoryId);
}
