package com.swyp.picke.domain.admin.dto.funding.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TicketCodeWorkbookRequest(@NotEmpty List<@NotNull Long> ticketIds) { }
