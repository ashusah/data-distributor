package com.datadistributor.outadapter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CehResponseInitialEventId implements Serializable {

  @Column(name = "ceh_initial_event_id", length = 50)
  private String cehInitialEventId;

  @Column(name = "signal_id")
  private Long signalId;
}
