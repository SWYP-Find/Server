package com.swyp.picke.domain.funding.service;

import com.swyp.picke.domain.admin.dto.funding.request.ManualTumblbugBuyerRequest;
import com.swyp.picke.domain.admin.dto.funding.response.ManualTumblbugBuyerResponse;
import com.swyp.picke.domain.funding.dto.request.TumblbugSupporterImportRequest;
import com.swyp.picke.domain.funding.dto.request.TumblbugDigitalGiftDeliveryUpdateRequest;
import com.swyp.picke.domain.funding.dto.response.*;

/** XLSX-specific operations; shared ticket lifecycle belongs to domain.ticket/domain.admin. */
public interface FundingAdminService {
    TumblbugImportPreviewResponse previewTumblbugSupporters(TumblbugSupporterImportRequest request, byte[] file);
    TumblbugWorkbookInspectResponse inspectTumblbugWorkbook(byte[] file);
    TumblbugSupporterImportResponse importTumblbugSupporters(TumblbugSupporterImportRequest request, byte[] file, Long adminUserId);
    ManualTumblbugBuyerResponse createManualTumblbugBuyer(ManualTumblbugBuyerRequest request, Long adminUserId);
    byte[] issueTumblbugDigitalGiftCodes(TumblbugSupporterImportRequest request, byte[] file, Long adminUserId);
    TumblbugDigitalGiftDeliveryUpdateResponse updateTumblbugDigitalGiftDelivery(
            TumblbugDigitalGiftDeliveryUpdateRequest request, Long adminUserId);
}
