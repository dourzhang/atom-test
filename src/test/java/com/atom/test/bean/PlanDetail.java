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

    private BigDecimal waitReserveMoney;   //待还款金额

    private BigDecimal realDeposit;     //每次还款金额

    private BigDecimal reserveFee;     //预交手续费]

    private BigDecimal deposit;     //每次还款金额

    private String status;//还款状态  已还款:Repayment 未还款:unRepayment

    private Date date;

    private String outputCadNum;

    private BigDecimal rollMoney;

//    private BigDecimal outPutMoney;

    @Override
    public String toString() {

        StringBuffer buffer = new StringBuffer();

        buffer.append("PlanDetail{")
                .append(" planNum=").append(planNum).append(",")
                .append(outputCadNum).append("->").append(cardNum).append(",")
                .append(" rollMoney : ").append(rollMoney).append(",")
//                .append(" outPutMoney : ").append(outPutMoney).append(",")
                .append(" 还款 deposit : ").append(deposit).append(",")
                .append(" 手续费 reserveFee : ").append(reserveFee).append(",")
                .append(" 实际扣款 realDeposit : ").append(realDeposit).append(",")
                .append("剩余 waitReserveMoney:").append(waitReserveMoney)
                .append("}");

        return buffer.toString();
//        return "PlanDetail{" +
//                ", planNum='" + planNum + '\'' +
//                ", 卡号 cardNum='" + cardNum + '\'' +
////                ", reserveMoney=" + reserveMoney +
//                ", 还款 deposit=" + deposit +
//                ", 剩余 waitReserveMoney=" + waitReserveMoney +
//                ", date=" + date +
//                "}";
    }
}
