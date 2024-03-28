package com.ssafy.gallery.option.controller;

import com.ssafy.gallery.common.exception.ApiExceptionFactory;
import com.ssafy.gallery.common.response.ApiResponse;
import com.ssafy.gallery.option.dto.KakaoPayApproveResponse;
import com.ssafy.gallery.option.dto.KakaoPayReadyResponse;
import com.ssafy.gallery.option.exception.OptionExceptionEnum;
import com.ssafy.gallery.option.model.OptionBuyLog;
import com.ssafy.gallery.option.model.OptionCategory;
import com.ssafy.gallery.option.model.OptionStore;
import com.ssafy.gallery.option.service.OptionService;
import com.ssafy.gallery.redis.dto.KakaoPayReadyDto;
import com.ssafy.gallery.redis.repository.KakaoPayReadyRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/option")
@RestController
public class OptionController {
    private final OptionService optionService;
    private final KakaoPayReadyRepository kakaoPayReadyRepository;

    @Value("${pay.client.redirect_uri}")
    private String redirectUrl;

    @GetMapping("/list")
    ResponseEntity<ApiResponse<?>> optionList(HttpServletRequest request) {
        log.info("옵션 리스트 요청");

        int userId = (int) request.getAttribute("userId");
        List<OptionStore> optionList = optionService.getList();
        HashMap<Integer, OptionStore> result = new HashMap<>();
        for (OptionStore o : optionList) {
            result.put(o.getOptionId(), o);
        }

        List<OptionBuyLog> buyOptionList = optionService.getBuyOptionList(userId);
        for (OptionBuyLog o : buyOptionList) {
            result.get(o.getOptionId()).setPurchased(true);
        }

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(result.values()));
    }

    @GetMapping("/category")
    ResponseEntity<ApiResponse<?>> optionCategory() {
        log.info("옵션 카테고리 요청");
        List<OptionCategory> category = optionService.getCategory();
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(category));
    }

    @PostMapping("/payment/ready")
    ResponseEntity<ApiResponse<?>> paymentReadyReq(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        int userId = (int) request.getAttribute("userId");
        int optionId = (int) params.get("optionId");
        log.info("{} 유저의 {} 옵션 구매 요청", userId, optionId);

        // 이미 구매한 옵션 예외처리
        List<OptionBuyLog> buyOptionList = optionService.getBuyOptionList(userId);
        for (OptionBuyLog o : buyOptionList) {
            if (o.getOptionId() == optionId) {
                throw ApiExceptionFactory.fromExceptionEnum(OptionExceptionEnum.ALREADY_PURCHASED);
            }
        }

        // 존재하지 않는 옵션 예외처리
        Optional<OptionStore> option = optionService.getOption(optionId);
        if (option.isEmpty()) {
            throw ApiExceptionFactory.fromExceptionEnum(OptionExceptionEnum.NO_OPTION);
        }

        String optionName = option.get().getOptionName();
        int cost = option.get().getCost();
        KakaoPayReadyResponse kakaoPayReadyResponse = optionService.paymentReady(userId, optionName, optionId, cost);
        KakaoPayReadyDto kakaoPayReadyDto = KakaoPayReadyDto.builder().id(userId).tid(kakaoPayReadyResponse.tid()).build();
        kakaoPayReadyRepository.save(kakaoPayReadyDto);

        HashMap<String, String> result = new HashMap<>();
        result.put("nextRedirectPcUrl", kakaoPayReadyResponse.nextRedirectPcUrl());
        result.put("nextRedirectMobileUrl", kakaoPayReadyResponse.nextRedirectMobileUrl());

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(result));
    }

    @GetMapping("/payment/success")
    ResponseEntity<ApiResponse<?>> paymentApproveReq(@RequestParam("user_id") String userId, @RequestParam("pg_token") String pgToken) {
        log.info("결제 성공 - userId: {}, pgToken:{}", userId, pgToken);

        Optional<KakaoPayReadyDto> kakaoPayReadyDto = kakaoPayReadyRepository.findById(userId);
        if (kakaoPayReadyDto.isEmpty()) {
            throw ApiExceptionFactory.fromExceptionEnum(OptionExceptionEnum.NO_TID);
        }

        String tid = kakaoPayReadyDto.get().getTid();
        KakaoPayApproveResponse kakaoPayApproveResponse = optionService.paymentApprove(tid, pgToken);

        if (kakaoPayApproveResponse == null) {
            throw ApiExceptionFactory.fromExceptionEnum(OptionExceptionEnum.PAY_FAIL);
        }

        optionService.buyOption(Integer.parseInt(userId), Integer.parseInt(kakaoPayApproveResponse.itemCode()));

        return ResponseEntity.status(HttpStatus.FOUND).header("Location", redirectUrl).build();
    }

    @GetMapping("/payment/cancel")
    public void cancel() {
        log.info("결제 취소");
        throw ApiExceptionFactory.fromExceptionEnum(OptionExceptionEnum.PAY_CANCEL);
    }

    @GetMapping("/payment/fail")
    public void fail() {
        log.info("결제 실패");
        throw ApiExceptionFactory.fromExceptionEnum(OptionExceptionEnum.PAY_FAIL);
    }
}