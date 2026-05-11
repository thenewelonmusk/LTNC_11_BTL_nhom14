package com.auction.service.impl;

import com.auction.dto.AuctionRequest;
import com.auction.dto.AuctionResponse;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.repository.AuctionRepository;
import com.auction.repository.ItemRepository;
import com.auction.service.AuctionService;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionServiceImpl implements AuctionService {
    // Error messages
    private static final String ERROR_INVALID_REQUEST = "Yêu cầu không hợp lệ.";
    private static final String ERROR_ITEM_NOT_FOUND = "Không tìm thấy sản phẩm.";
    private static final String ERROR_NOT_OWNER = "Bạn không phải chủ sản phẩm này.";
    private static final String ERROR_INVALID_PRICE = "Giá khởi điểm phải > 0.";
    private static final String ERROR_INVALID_TIME = "Thời gian không hợp lệ.";
    private static final String ERROR_TIME_IN_PAST = "Thời gian bắt đầu không thể ở quá khứ.";
    private static final String ERROR_AUCTION_NOT_FOUND = "Không tìm thấy phiên đấu giá.";
    private static final String ERROR_CANNOT_CANCEL = "Không thể hủy phiên đã kết thúc.";
    private static final String ERROR_CANNOT_FINISH = "Không thể kết thúc phiên đã đóng.";
    private static final String ERROR_STILL_RUNNING = "Phiên đấu giá chưa kết thúc.";
    private static final String ERROR_SAVE_FAILED = "Lỗi hệ thống, không thể lưu.";
    private static final String ERROR_ITEM_ALREADY_AUCTIONED = "Sản phẩm đã có phiên đấu giá đang mở.";

    // Success messages
    private static final String SUCCESS_OPEN = "Mở phiên đấu giá thành công.";
    private static final String SUCCESS_CANCEL = "Hủy phiên đấu giá thành công.";
    private static final String SUCCESS_FINISH = "Kết thúc phiên đấu giá thành công.";
    private static final String SUCCESS_FOUND = "OK.";

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;

    public AuctionServiceImpl(AuctionRepository auctionRepository, ItemRepository itemRepository) {
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public AuctionResponse openAuction(AuctionRequest request, Long sellerId) {
        String error = validateRequest(request);
        if (error != null) {
            return new AuctionResponse(false,error,null);
        }

        Item item = itemRepository.findById(request.getItemId());
        if (item == null) {
            return new AuctionResponse(false,ERROR_ITEM_NOT_FOUND,null);
        }

        if (!sellerId.equals(item.getSellerId())) {
            return new AuctionResponse(false,ERROR_NOT_OWNER,null);
        }

        List<Auction> existingAuctions = auctionRepository.findByItemId(request.getItemId());
        for (Auction a : existingAuctions) {
           if (a.getStatus() == AuctionStatus.OPEN || a.getStatus() == AuctionStatus.RUNNING) {
               return new AuctionResponse(false,ERROR_ITEM_ALREADY_AUCTIONED,null);
           }
        }

        Auction auction = new Auction();
        auction.setItemId(request.getItemId());
        auction.setSellerId(sellerId);
        auction.setStartingPrice(request.getStartingPrice());
        auction.setCurrentPrice(request.getStartingPrice());
        auction.setStartTime(request.getStartTime());
        auction.setEndTime(request.getEndTime());

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(request.getStartTime())) {
            auction.setStatus(AuctionStatus.OPEN);
        } else {
            auction.setStatus(AuctionStatus.RUNNING);
        }

        boolean ok = auctionRepository.save(auction);
        if (!ok) {
            return new AuctionResponse(false,ERROR_SAVE_FAILED,null);
        }

        return new AuctionResponse(true,SUCCESS_OPEN,auction);
    }

    @Override
    public AuctionResponse cancelAuction(Long auctionId, Long sellerId) {
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            return new AuctionResponse(false,ERROR_AUCTION_NOT_FOUND,null);
        }

        if (!sellerId.equals(auction.getSellerId())) {
            return new AuctionResponse(false,ERROR_NOT_OWNER,null);
        }

        if (auction.getStatus() == AuctionStatus.FINISHED
                || auction.getStatus() == AuctionStatus.PAID
                || auction.getStatus() == AuctionStatus.CANCELED) {
            return new AuctionResponse(false, ERROR_CANNOT_CANCEL, null);
        }

        auction.setStatus(AuctionStatus.CANCELED);
        boolean ok = auctionRepository.save(auction);
        if (!ok) {
            return new AuctionResponse(false,ERROR_SAVE_FAILED,null);
        }

        return new AuctionResponse(true,SUCCESS_CANCEL,auction);
    }

    @Override
    public AuctionResponse finishAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            return new AuctionResponse(false,ERROR_AUCTION_NOT_FOUND,null);
        }

        if (LocalDateTime.now().isBefore(auction.getEndTime())) {
            return new AuctionResponse(false, ERROR_STILL_RUNNING,null);
        }

        if (auction.getStatus() == AuctionStatus.FINISHED
            || auction.getStatus() == AuctionStatus.PAID
            || auction.getStatus() == AuctionStatus.CANCELED) {
            return new AuctionResponse(false,ERROR_CANNOT_FINISH,null);
        }

        auction.setStatus(AuctionStatus.FINISHED);

        boolean ok = auctionRepository.save(auction);
        if (!ok) {
            return new AuctionResponse(false,ERROR_SAVE_FAILED,null);
        }

        return new AuctionResponse(true,SUCCESS_FINISH,auction);
    }

    @Override
    public void refreshStatus(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) return;

        LocalDateTime now = LocalDateTime.now();
        AuctionStatus current = auction.getStatus();

        // OPEN -> RUNNING
        if (current == AuctionStatus.OPEN && !now.isBefore(auction.getStartTime())) {
            auction.setStatus(AuctionStatus.RUNNING);
            auctionRepository.save(auction);
        }

        //RUNNING -> FINISHED
        else if (current == AuctionStatus.RUNNING && now.isAfter(auction.getEndTime())) {
            auction.setStatus(AuctionStatus.FINISHED);
            auctionRepository.save(auction);
        }
    }

    @Override
    public AuctionResponse findAuctionById(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId);

        if (auction == null) {
            return new AuctionResponse(false, ERROR_AUCTION_NOT_FOUND, null);
        }
        return new AuctionResponse(true, SUCCESS_FOUND, auction);
    }

    @Override
    public List<Auction> getAllAuctions() {
        return auctionRepository.findALl();
    }

    @Override
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        return auctionRepository.findByStatus(status);
    }

    @Override
    public List<Auction> getAuctionsBySeller(Long sellerId) {
        return auctionRepository.findBySellerId(sellerId);
    }

    public String validateRequest(AuctionRequest req) {
        if (req == null)  return ERROR_INVALID_REQUEST;
        if (req.getItemId() == null) return ERROR_INVALID_REQUEST;
        if (req.getStartingPrice() <= 0) return ERROR_INVALID_PRICE;
        if (req.getStartTime() == null || req.getEndTime() == null) return ERROR_INVALID_TIME;
        if (!req.getEndTime().isAfter(req.getStartTime())) return ERROR_INVALID_TIME;
        if (req.getStartTime().isBefore(LocalDateTime.now().minusMinutes(1))) return ERROR_TIME_IN_PAST;
        return null;
    }

}
