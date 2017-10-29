/**
 * onway.com Inc.
 * Copyright (c) 2016-2017 All Rights Reserved.
 */
package com.atom.test.bean;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Crystal
 * @version $Id: PlanDetail.java, v 0.1 2017年10月16日 下午2:16:53 Crystal Exp $
 */
@Data
public class PlanDetail {

    private Integer id;

    private String planNum;

    private String userId;

    private String username;

    private String cardNum;

    private String payStartDate;

    private BigDecimal reserveMoney;   //预定还款额

    private BigDecimal waitReserveMoney;   //预定还款额

    private BigDecimal deposit;     //每次还款金额

    private BigDecimal reserveFee;     //预交手续费

    private String status;//还款状态  已还款:Repayment 未还款:unRepayment

    private Date date;

    @Override
    public String toString() {
        return "PlanDetail{" +
                "id=" + id +
                ", planNum='" + planNum + '\'' +
                ", 卡号 cardNum='" + cardNum + '\'' +
//                ", reserveMoney=" + reserveMoney +
                ", 还款 deposit=" + deposit +
                ", 剩余 waitReserveMoney=" + waitReserveMoney +
                ", date=" + date +
                '}';
    }
}
