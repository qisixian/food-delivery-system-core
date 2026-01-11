package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (begin.isBefore(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();
            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }
        String dateStr = StringUtils.join(dateList, ",");
        String turnoverStr = StringUtils.join(turnoverList, ",");
        return TurnoverReportVO.builder()
                .dateList(dateStr)
                .turnoverList(turnoverStr)
                .build();
    }


    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (begin.isBefore(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();
            Map map = new HashMap();
            map.put("endTime", endTime);
            // 总用户数
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
            // 新增用户数
            map.put("beginTime", beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
        }

        String dateStr = StringUtils.join(dateList, ",");
        String totalUserStr = StringUtils.join(totalUserList, ",");
        String newUserStr = StringUtils.join(newUserList, ",");
        return UserReportVO.builder()
                .dateList(dateStr)
                .totalUserList(totalUserStr)
                .newUserList(newUserStr)
                .build();
    }


    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (begin.isBefore(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> totalOrderList = new ArrayList<>();
        List<Integer> completedOrderList = new ArrayList<>();
        Integer totalOrderCount = 0;
        Integer completedOrderCount = 0;
        for(LocalDate date : dateList) {
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();
            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            // 总订单数
            Integer totalOrder = orderMapper.countByMap(map);
            totalOrderList.add(totalOrder);
            totalOrderCount += totalOrder;
            // 完成订单数
            map.put("status", Orders.COMPLETED);
            Integer completedOrder = orderMapper.countByMap(map);
            completedOrderList.add(completedOrder);
            completedOrderCount += completedOrder;
        }
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : (completedOrderCount * 100.0) / totalOrderCount;
        String dateStr = StringUtils.join(dateList, ",");
        String totalOrderStr = StringUtils.join(totalOrderList, ",");
        String completedOrderStr = StringUtils.join(completedOrderList, ",");

        return OrderReportVO.builder()
                .dateList(dateStr)
                .orderCountList(totalOrderStr)
                .validOrderCountList(completedOrderStr)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(completedOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = begin.atStartOfDay();
        LocalDateTime endTime = end.plusDays(1).atStartOfDay();
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        StringJoiner nameJoiner = new StringJoiner(",");
        StringJoiner numberJoiner = new StringJoiner(",");
        for (GoodsSalesDTO sales : salesTop10) {
            nameJoiner.add(sales.getName());
            numberJoiner.add(String.valueOf(sales.getNumber()));
        }
        return SalesTop10ReportVO.builder()
                .nameList(nameJoiner.toString())
                .numberList(numberJoiner.toString())
                .build();
    }

    public void exportBusinessData(HttpServletResponse response) {
        LocalDateTime dateBegin = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime dateEnd = LocalDate.now().atStartOfDay();
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(dateBegin, dateEnd);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);
            // 填充数据
            XSSFSheet sheet = excel.getSheet("Sheet1");
            // 时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + " 至 " + dateEnd);
            // 营业额
            sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
            // 订单完成率
            sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            // 新增用户数
            sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());
            // 有效订单数
            sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            // 平均客单价
            sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());
            // 填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.toLocalDate().plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
                XSSFRow row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            // 通过输出流把文件写回浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            out.close();
            excel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
