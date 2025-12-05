package com.datadistributor.outadapter.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite-key entity storing the CEH initial event identifier returned for a given signal.
 */
@Entity
@Table(name = "ceh_response_initial_event_id")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CehResponseInitialEventEntity {

  @EmbeddedId
  private CehResponseInitialEventId id;
}
