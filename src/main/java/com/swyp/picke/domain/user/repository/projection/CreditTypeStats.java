package com.swyp.picke.domain.user.repository.projection;

import com.swyp.picke.domain.user.enums.CreditType;

public interface CreditTypeStats {
    CreditType getCreditType();
    long getCount();
    long getTotalAmount();
}
