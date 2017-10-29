package com.atom.test;

import com.atom.test.bean.ParamCard;
import com.atom.test.bean.PlanDetail;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class CardTest {

    private static final Logger logger = LoggerFactory.getLogger(CardTest.class);

    /**
     * 手续费利息
     */
    private static final BigDecimal INTEREST = new BigDecimal(0.0085);
    /**
     * 占比
     */
    private static final BigDecimal ACTUAL_RATE = new BigDecimal(0.9915);
    /**
     * 手续费利息
     */
    private static final BigDecimal PROXY_PAY = new BigDecimal(0.5);
    /**
     * 代付费 边界值
     */
    private BigDecimal border = new BigDecimal(0.10);

    private static final BigDecimal TEN = new BigDecimal(10);

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    private DateFormat df = new SimpleDateFormat(DATE_PATTERN);

    List<ParamCard> paramCards = new ArrayList<>();

    private BigDecimal refundBase = new BigDecimal(1000);

    @Before
    public void init() throws ParseException {

        ParamCard paramCard1 = new ParamCard();
        paramCard1.setCardNum("11111");
        paramCard1.setRefundAmount(new BigDecimal(10000));
        paramCard1.setRefundBase(refundBase);
        paramCard1.setPayStartDate(DateUtils.parseDate("2017-11-01", DATE_PATTERN));
        paramCard1.setPayEndDate(DateUtils.parseDate("2017-11-08", DATE_PATTERN));

        ParamCard paramCard2 = new ParamCard();
        paramCard2.setCardNum("22222");
        paramCard2.setRefundAmount(new BigDecimal(5000));
        paramCard2.setRefundBase(refundBase);
        paramCard2.setPayStartDate(DateUtils.parseDate("2017-11-01", DATE_PATTERN));
        paramCard2.setPayEndDate(DateUtils.parseDate("2017-11-11", DATE_PATTERN));

        ParamCard paramCard3 = new ParamCard();
        paramCard3.setCardNum("33333");
        paramCard3.setRefundAmount(new BigDecimal(20000));
        paramCard3.setRefundBase(refundBase);
        paramCard3.setPayStartDate(DateUtils.parseDate("2017-11-02", DATE_PATTERN));
        paramCard3.setPayEndDate(DateUtils.parseDate("2017-11-09", DATE_PATTERN));

        paramCards.add(paramCard1);
        paramCards.add(paramCard2);
        paramCards.add(paramCard3);
    }

    @Test
    public void clientTest() {

        Map<String, BigDecimal> map = new HashMap<>();

        //递归 模版方法 templateTest
        Date startDate = paramCards.stream().min(Comparator.comparing(ParamCard::getPayStartDate)).map(ParamCard::getPayStartDate).get();
        Date endDate = paramCards.stream().max(Comparator.comparing(ParamCard::getPayEndDate)).map(ParamCard::getPayEndDate).get();

        BigDecimal total = BigDecimal.ZERO;
        for (ParamCard paramCard : paramCards) {
            total = total.add(paramCard.getRefundAmount().multiply(INTEREST).setScale(2, BigDecimal.ROUND_HALF_UP));
            if (paramCard.getRefundRate().compareTo(border) <= 0) {
                BigDecimal proxyCost = (paramCard.getRefundAmount().divide(paramCard.getRefundBase()).subtract(TEN)).multiply(PROXY_PAY);
                total = total.add(proxyCost);
            }
        }
        total = total.add(refundBase);

        logger.info("预计总还款额:{}\n", total);

        Date vernier = startDate;
        //公式计数器
        int totalCount = 0;
        int count = 0;
        List<PlanDetail> planDetails = new ArrayList<>();
        PlanDetail snapshot = new PlanDetail();
        snapshot.setDeposit(total);
        Map<String, BigDecimal> numDepositMap = new HashMap<>();
        Map<String, BigDecimal> waitReserveMap = new HashMap<>();
        while (vernier.getTime() <= endDate.getTime()) {
            int circleCount = 1;
            List<ParamCard> filterParamCards = filterByDate(paramCards, vernier).stream().sorted(Comparator.comparing(ParamCard::getRefundAmount).reversed()).collect(toList());
            Integer maxPreCount = filterParamCards.stream().max(Comparator.comparing(ParamCard::getPreCount)).map(ParamCard::getPreCount).get().intValue();
            List<ParamCard> remainderList = filterParamCards.stream().filter(paramCard -> paramCard.getRemainder() != 0).collect(toList());
            //最大余数
            Integer maxRemainder = maxPreCount;
            if (!CollectionUtils.isEmpty(remainderList) && count < maxRemainder) {
                maxRemainder += 1;
            }
            logger.info("------- day vernier:{} -------", df.format(vernier));
            while (circleCount <= maxRemainder) {
                for (ParamCard paramCard : filterParamCards) {
                    //整除
                    Boolean pre = paramCard.getRemainder() == 0 && circleCount > paramCard.getPreCount().intValue();
                    //非整除
                    Boolean remainder = paramCard.getRemainder() > 0 && circleCount > paramCard.getPreCount().intValue() + 1;
                    if (pre || remainder) {
                        continue;
                    }
                    PlanDetail planDetail = new PlanDetail();
                    planDetail.setCardNum(paramCard.getCardNum());
                    planDetail.setPlanNum(count + "");
                    BigDecimal deposit = snapshot.getDeposit().multiply(ACTUAL_RATE.pow(totalCount)).setScale(2, BigDecimal.ROUND_HALF_UP);
                    BigDecimal deposit2 = total.multiply(ACTUAL_RATE.pow(totalCount)).setScale(2, BigDecimal.ROUND_HALF_UP);
                    BigDecimal tmp = null == numDepositMap.get(paramCard.getCardNum()) ? BigDecimal.ZERO : numDepositMap.get(paramCard.getCardNum());
                    BigDecimal thisDeposit = tmp.add(deposit2);

                    BigDecimal waitReserve = null == waitReserveMap.get(paramCard.getCardNum()) ? paramCard.getRefundAmount() : waitReserveMap.get(paramCard.getCardNum());
                    if (deposit2.compareTo(waitReserve) >= 0) {
                        planDetail.setDeposit(waitReserve);
                        planDetail.setWaitReserveMoney(BigDecimal.ZERO);
                        numDepositMap.put(paramCard.getCardNum(), waitReserve);
                        waitReserveMap.put(paramCard.getCardNum(), BigDecimal.ZERO);
                    } else {
                        planDetail.setDeposit(deposit2);
                        planDetail.setWaitReserveMoney(paramCard.getRefundAmount().subtract(thisDeposit));
                        numDepositMap.put(paramCard.getCardNum(), thisDeposit);
                        waitReserveMap.put(paramCard.getCardNum(), planDetail.getWaitReserveMoney());
                    }
                    planDetail.setDate(vernier);
                    snapshot = planDetail;
                    totalCount++;
                    logger.info("planDetail:{}", planDetail);
                    planDetails.add(planDetail);
                }
                circleCount++;
            }

            vernier = DateUtils.addDays(vernier, 1);
            count++;
        }


        logger.info("********** result **********\n");
//        planDetails.forEach(planDetail -> {
//            logger.info("info:{}", planDetails);
//        });
    }

    private List<ParamCard> filterByDate(List<ParamCard> filterParamCards, Date vernier) {
        List<ParamCard> list = new ArrayList<>();
        for (ParamCard paramCard : filterParamCards) {
            if (vernier.getTime() >= paramCard.getPayStartDate().getTime() && vernier.getTime() <= paramCard.getPayEndDate().getTime()) {
                list.add(paramCard);
            }
        }
        return list;
    }


}
