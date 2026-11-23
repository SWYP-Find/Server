package com.swyp.picke.domain.funding.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TumblbugSupporterRowsImportRequest(
        @NotNull @Valid TumblbugSupporterImportRequest options,
        @NotEmpty @Size(max = 5000) List<@NotNull @Valid TumblbugSupporterRowInput> rows
) { }
