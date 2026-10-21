package com.swyp.picke.domain.funding.service;

import com.swyp.picke.domain.funding.dto.request.TumblbugSupporterImportRequest;
import com.swyp.picke.domain.funding.dto.request.TumblbugDigitalGiftDeliveryUpdateRequest;
import com.swyp.picke.domain.funding.dto.response.*;

/** XLSX-specific operations; shared ticket lifecycle belongs to domain.ticket/domain.admin. */
public interface FundingAdminService {
    TumblbugImportPreviewResponse previewTumblbugSupporters(TumblbugSupporterImportRequest request, byte[] file);
    TumblbugWorkbookInspectResponse inspectTumblbugWorkbook(byte[] file);
    byte[] issueTumblbugDigitalGiftCodes(TumblbugSupporterImportRequest request, byte[] file, Long adminUserId);
    TumblbugDigitalGiftDeliveryUpdateResponse updateTumblbugDigitalGiftDelivery(
            TumblbugDigitalGiftDeliveryUpdateRequest request, Long adminUserId);
}
