package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.StatusRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusRepository extends JpaRepository<StatusRecord, Long> {
}
