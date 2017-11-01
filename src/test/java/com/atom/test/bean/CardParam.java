package com.atom.test.bean;

import lombok.Data;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 卡参数
 */
@Data
public class CardParam {

    private Long id;
    /**
     * 卡号
     */
    private String cardNum;
    /**
     * 还款额
     */
    private BigDecimal refundAmount;
    /**
     * 开始还款日期
     */
    private Date payStartDate;
    /**
     * 结束还款日期
     */
    private Date payEndDate;
    /**
     * 还款基数
     */
    private BigDecimal refundBase;
    /**
     * 还比率
     */
    private BigDecimal refundRate;

    /**
     * 余额getRefundBase
     */
//    private BigDecimal balance;
//    public BigDecimal getRefundRate() {
//        return refundBase.divide(refundAmount, 2, BigDecimal.ROUND_DOWN).multiply(new BigDecimal(0.01)).setScale(2, BigDecimal.ROUND_DOWN);
//    }
    public BigDecimal getPreCount() {
        //预判次数
        return refundAmount.divide(refundBase, BigDecimal.ROUND_UP);
    }

    public BigDecimal getAvePreCount() {
        //预判次数
        BigDecimal count = refundAmount.divide(refundBase, BigDecimal.ROUND_DOWN);
        BigDecimal dayDiff = null;
        try {
            dayDiff = daysBetween(payStartDate, payEndDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //比天数多 按天均分
        if (count.compareTo(dayDiff) > 0) {
            return count.divide(dayDiff, BigDecimal.ROUND_DOWN);
        }
        return BigDecimal.ONE;
    }

    public Integer getRemainder() {

        //预判次数
        BigDecimal count = refundAmount.divide(refundBase, BigDecimal.ROUND_DOWN);
        BigDecimal dayDiff = null;
        try {
            dayDiff = daysBetween(payStartDate, payEndDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //求余数
        if (count.compareTo(dayDiff) > 0) {
            int remainder = (count.divideAndRemainder(dayDiff)[1]).intValue();
            return remainder;
        }
        return 0;
    }

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    private DateFormat df = new SimpleDateFormat(DATE_PATTERN);


    /**
     * 计算两个日期之间相差的天数
     *
     * @param d1 较小的时间
     * @param d2 较大的时间
     * @return 相差天数
     * @throws ParseException
     */
    public BigDecimal daysBetween(Date d1, Date d2) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        d1 = sdf.parse(sdf.format(d1));
        d2 = sdf.parse(sdf.format(d2));

        Calendar cal = Calendar.getInstance();
        cal.setTime(d1);
        long time1 = cal.getTimeInMillis();
        cal.setTime(d2);
        long time2 = cal.getTimeInMillis();
        long betweenDays = (time2 - time1) / (1000 * 3600 * 24);
        Integer diff = Integer.parseInt(String.valueOf(betweenDays));
        return new BigDecimal(diff + 1);
    }

    @Override
    public String toString() {
        return "ParamCard{" +
                "id=" + id +
                ", cardNum='" + cardNum + '\'' +
                ", refundRate=" + refundRate +
                ", refundAmount=" + refundAmount +
                ", payStartDate=" + payStartDate +
                ", payEndDate=" + payEndDate +
                ", refundBase=" + refundBase +
                ", refundBase=" + refundBase +
                ", preCount=" + getAvePreCount() +
                ", remainder=" + getRemainder() +
                '}';
    }
}
