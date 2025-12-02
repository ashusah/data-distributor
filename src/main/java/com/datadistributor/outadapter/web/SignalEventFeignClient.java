package com.datadistributor.outadapter.web;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "signalEventClient",
    url = "${data-distributor.external-api.base-url}"
)
public interface SignalEventFeignClient {

  @PostMapping("${data-distributor.external-api.write-signal-path:/create-signal/write-signal}")
  Map<String, Object> postSignalEvent(@RequestBody Map<String, Object> payload);
}
