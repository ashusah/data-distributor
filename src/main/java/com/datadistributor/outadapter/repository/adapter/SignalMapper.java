package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.Signal;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SignalMapper {

  public Signal toDomain(SignalJpaEntity entity) {
    if (entity == null) return null;
    Signal signal = new Signal();
    signal.setSignalId(entity.getSignalId());
    signal.setAgreementId(entity.getAgreementId());
    signal.setSignalStartDate(entity.getSignalStartDate());
    signal.setSignalEndDate(entity.getSignalEndDate());
    return signal;
  }
}
