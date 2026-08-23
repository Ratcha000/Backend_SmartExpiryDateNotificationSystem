package com.app.expiry_system.purchase.service;

import com.app.expiry_system.purchase.entity.PurchaseRecommendation;
import com.app.expiry_system.purchase.entity.PurchaseRecommendationRun;
import com.app.expiry_system.purchase.entity.PurchaseRunStatus;
import com.app.expiry_system.purchase.repository.PurchaseRecommendationRepository;
import com.app.expiry_system.purchase.repository.PurchaseRecommendationRunRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * เขียนผลของการ generate ลง database แยกออกจาก {@link PurchasePlanningService}
 * เพื่อไม่ให้ transaction ถูกถือค้างระหว่างรอ response จาก AI
 * และเพื่อให้บันทึกรอบที่ล้มเหลวได้โดยไม่ถูก rollback ไปพร้อมกับ exception.
 */
@Component
public class PurchaseRunWriter {

    private final PurchaseRecommendationRunRepository runRepository;
    private final PurchaseRecommendationRepository recommendationRepository;

    public PurchaseRunWriter(PurchaseRecommendationRunRepository runRepository,
                             PurchaseRecommendationRepository recommendationRepository) {
        this.runRepository = runRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional
    public PurchaseRecommendationRun saveSuccessfulRun(PurchaseRecommendationRun run,
                                                       List<PurchaseRecommendation> items,
                                                       int maxRuns) {
        run.setStatus(PurchaseRunStatus.SUCCESS);
        run.setItemCount(items.size());
        run.setTotalBuyItems((int) items.stream()
                .filter(item -> item.getRecommendedBuyQuantity().compareTo(BigDecimal.ZERO) > 0)
                .count());

        PurchaseRecommendationRun savedRun = runRepository.save(run);
        items.forEach(item -> {
            item.setRunId(savedRun.getId());
            recommendationRepository.save(item);
        });

        // แถวจากดีไซน์เดิมที่ยังไม่มี runId ไม่อยู่ในประวัติรอบใด จึงล้างทิ้งเพื่อไม่ให้ค้างเป็นข้อมูลกำพร้า
        recommendationRepository.deleteByRestaurantIdAndRunIdIsNull(savedRun.getRestaurantId());

        pruneOldRuns(savedRun.getRestaurantId(), maxRuns);
        return savedRun;
    }

    @Transactional
    public PurchaseRecommendationRun saveFailedRun(PurchaseRecommendationRun run, String errorMessage, int maxRuns) {
        run.setStatus(PurchaseRunStatus.FAILED);
        run.setItemCount(0);
        run.setTotalBuyItems(0);
        run.setErrorMessage(truncateError(errorMessage));

        PurchaseRecommendationRun savedRun = runRepository.save(run);
        pruneOldRuns(savedRun.getRestaurantId(), maxRuns);
        return savedRun;
    }

    @Transactional
    public void pruneOldRuns(String restaurantId, int maxRuns) {
        List<PurchaseRecommendationRun> expired =
                runRepository.findByRestaurantIdOrderByGeneratedAtDesc(restaurantId).stream()
                        .skip(maxRuns)
                        .collect(Collectors.toList());
        if (expired.isEmpty()) {
            return;
        }

        List<String> expiredIds = expired.stream()
                .map(PurchaseRecommendationRun::getId)
                .collect(Collectors.toList());
        recommendationRepository.deleteByRunIdIn(expiredIds);
        runRepository.deleteAll(expired);
    }

    private String truncateError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Unknown error";
        }
        return errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage;
    }
}
