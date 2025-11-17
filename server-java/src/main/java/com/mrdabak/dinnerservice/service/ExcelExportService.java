package com.mrdabak.dinnerservice.service;

import com.mrdabak.dinnerservice.model.*;
import com.mrdabak.dinnerservice.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final DinnerTypeRepository dinnerTypeRepository;
    private final MenuItemRepository menuItemRepository;

    public ExcelExportService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                             UserRepository userRepository, DinnerTypeRepository dinnerTypeRepository,
                             MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.dinnerTypeRepository = dinnerTypeRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public byte[] exportOrdersToExcel(String status) throws IOException {
        List<Order> orders;
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findAll();
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("주문 내역");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Create data style
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "주문 ID", "고객명", "고객 전화번호", "디너명", "서빙 스타일",
            "배달 시간", "배달 주소", "총 가격", "주문 상태", "결제 상태", "결제 방법", "주문 일시"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int rowNum = 1;
        
        for (Order order : orders) {
            Row row = sheet.createRow(rowNum++);
            
            // Order ID
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(order.getId());
            cell0.setCellStyle(dataStyle);
            
            // Customer info
            User customer = userRepository.findById(order.getUserId()).orElse(null);
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(customer != null ? customer.getName() : "");
            cell1.setCellStyle(dataStyle);
            
            Cell cell2 = row.createCell(2);
            cell2.setCellValue(customer != null ? customer.getPhone() : "");
            cell2.setCellStyle(dataStyle);
            
            // Dinner info
            DinnerType dinner = dinnerTypeRepository.findById(order.getDinnerTypeId()).orElse(null);
            Cell cell3 = row.createCell(3);
            cell3.setCellValue(dinner != null ? dinner.getName() : "");
            cell3.setCellStyle(dataStyle);
            
            // Serving style
            Cell cell4 = row.createCell(4);
            String servingStyle = order.getServingStyle();
            String styleName = switch (servingStyle) {
                case "simple" -> "심플";
                case "grand" -> "그랜드";
                case "deluxe" -> "디럭스";
                default -> servingStyle;
            };
            cell4.setCellValue(styleName);
            cell4.setCellStyle(dataStyle);
            
            // Delivery time
            Cell cell5 = row.createCell(5);
            cell5.setCellValue(order.getDeliveryTime());
            cell5.setCellStyle(dataStyle);
            
            // Delivery address
            Cell cell6 = row.createCell(6);
            cell6.setCellValue(order.getDeliveryAddress());
            cell6.setCellStyle(dataStyle);
            
            // Total price
            Cell cell7 = row.createCell(7);
            cell7.setCellValue(order.getTotalPrice());
            cell7.setCellStyle(dataStyle);
            
            // Order status
            Cell cell8 = row.createCell(8);
            String statusName = switch (order.getStatus()) {
                case "pending" -> "대기중";
                case "cooking" -> "조리중";
                case "ready" -> "준비완료";
                case "out_for_delivery" -> "배달중";
                case "delivered" -> "배달완료";
                case "cancelled" -> "취소됨";
                default -> order.getStatus();
            };
            cell8.setCellValue(statusName);
            cell8.setCellStyle(dataStyle);
            
            // Payment status
            Cell cell9 = row.createCell(9);
            String paymentStatusName = switch (order.getPaymentStatus()) {
                case "pending" -> "대기중";
                case "paid" -> "결제완료";
                case "failed" -> "결제실패";
                case "refunded" -> "환불됨";
                default -> order.getPaymentStatus();
            };
            cell9.setCellValue(paymentStatusName);
            cell9.setCellStyle(dataStyle);
            
            // Payment method
            Cell cell10 = row.createCell(10);
            cell10.setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod() : "");
            cell10.setCellStyle(dataStyle);
            
            // Created at
            Cell cell11 = row.createCell(11);
            if (order.getCreatedAt() != null) {
                cell11.setCellValue(order.getCreatedAt().format(formatter));
            }
            cell11.setCellStyle(dataStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
}



