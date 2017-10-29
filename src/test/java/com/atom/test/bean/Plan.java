package com.atom.test.bean;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Plan {

    private Integer id;            //序号
    private String userId;        //用户id
    private String plan_num;      //计划单号
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date pay_start_date; //开始还款日期
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date pay_end_date;  //结束还款日期
    private BigDecimal reserve_money; //预定还款额
    private BigDecimal deposit;       //保证金
    private BigDecimal reserve_fee;   //预交手续费

    private String username;
    private int status;
}
