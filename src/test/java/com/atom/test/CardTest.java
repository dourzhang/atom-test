package com.atom.test;

import com.atom.test.bean.CardParam;
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
     * 服务费率
     */
    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal(0.0085);
    /**
     * 实际还款费率
     */
    private static final BigDecimal ACTUAL_REFUND_RATE = new BigDecimal(0.9915);
    /**
     * 代理费
     */
    private static final BigDecimal PROXY_FEE = BigDecimal.ONE;

    /**
     * 代付费界限值 低于10%收取代付费
     */
    private BigDecimal PROXY_BOUND = new BigDecimal(0.10);

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private DateFormat df = new SimpleDateFormat(DATE_PATTERN);

    /**
     * 参数 用户传入每张卡信息
     */
    private List<CardParam> paramCards = new ArrayList<>();
    /**
     * 参数 还款基数
     */
    private BigDecimal refundBase = new BigDecimal(1000);

    @Before
    public void init() throws ParseException {

        CardParam paramCard1 = new CardParam();
        paramCard1.setCardNum("11111");
        paramCard1.setRefundAmount(new BigDecimal(10000));
        paramCard1.setRefundBase(refundBase);
        paramCard1.setPayStartDate(DateUtils.parseDate("2017-11-01", DATE_PATTERN));
        paramCard1.setPayEndDate(DateUtils.parseDate("2017-11-08", DATE_PATTERN));

        CardParam paramCard2 = new CardParam();
        paramCard2.setCardNum("22222");
        paramCard2.setRefundAmount(new BigDecimal(5000));
        paramCard2.setRefundBase(refundBase);
        paramCard2.setPayStartDate(DateUtils.parseDate("2017-11-01", DATE_PATTERN));
        paramCard2.setPayEndDate(DateUtils.parseDate("2017-11-11", DATE_PATTERN));

        CardParam paramCard3 = new CardParam();
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

        //计算所有卡还款开始至结束日期
        Date startDate = paramCards.stream().min(Comparator.comparing(CardParam::getPayStartDate)).map(CardParam::getPayStartDate).get();
        Date endDate = paramCards.stream().max(Comparator.comparing(CardParam::getPayEndDate)).map(CardParam::getPayEndDate).get();
        //计算需要支付的所有费用
        BigDecimal initRollMoney = BigDecimal.ZERO;
        for (CardParam paramCard : paramCards) {
            initRollMoney = initRollMoney.add(paramCard.getRefundAmount().multiply(SERVICE_FEE_RATE).setScale(2, BigDecimal.ROUND_DOWN));
            if (paramCard.getPreCount().compareTo(BigDecimal.TEN) > 0) {
                BigDecimal count = paramCard.getPreCount().subtract(BigDecimal.TEN);
                initRollMoney = initRollMoney.add(count.multiply(PROXY_FEE));
            }
        }
        initRollMoney = initRollMoney.add(refundBase);

        logger.info("预计总还款额:{}\n", initRollMoney);

        Date vernier = startDate;
        //公式计数器
        int totalCount = 0;
        //plan count
        int count = 0;

        List<PlanDetail> planDetails = new ArrayList<>();
        //上次支付
        Map<String, BigDecimal> numDepositMap = new HashMap<>();
        //记录上次待支付
        Map<String, BigDecimal> waitReserveMap = new HashMap<>();
        //还款天数
        Map<String, Integer> dayCountMap = new HashMap<>();
        //每张卡上次计划
//        Map<String, PlanDetail> cardSnapshotMap = new HashMap<>();
        //每张卡余额
//        Map<String, BigDecimal> balanceMap = new HashMap<>();
        //上次计划
        PlanDetail snapshotPlanDetail = new PlanDetail();
        snapshotPlanDetail.setPlanNum("000");
        snapshotPlanDetail.setCardNum("init");
        snapshotPlanDetail.setOutputCadNum("00000");
        snapshotPlanDetail.setRollMoney(initRollMoney);
        snapshotPlanDetail.setReserveFee(BigDecimal.ZERO);
        //按天数遍历计算
        while (vernier.getTime() <= endDate.getTime()) {
            int refundCount = 1;
            List<CardParam> filterParamCards = filterByDate(paramCards, vernier, dayCountMap).stream().sorted(Comparator.comparing(CardParam::getRefundAmount).reversed()).collect(toList());
            Integer maxPreCount = filterParamCards.stream().max(Comparator.comparing(CardParam::getAvePreCount)).map(CardParam::getAvePreCount).get().intValue();
            List<CardParam> remainderList = filterParamCards.stream().filter(paramCard -> paramCard.getRemainder() != 0).collect(toList());
            //每一天最多还款次数
            Integer maxRemainder = maxPreCount;
            if (!CollectionUtils.isEmpty(remainderList) && refundCount <= (maxPreCount + 1)) {
                maxRemainder += 1;
            }
            logger.info("------- day vernier:{} -------", df.format(vernier));

            for (CardParam paramCard : filterParamCards) {
                Integer dayCount = dayCountMap.get(paramCard.getCardNum());
                dayCountMap.put(paramCard.getCardNum(), null == dayCount ? 1 : dayCount + 1);
                logger.info("卡号:{},还款天数:{}", paramCard.getCardNum(), null == dayCount ? 1 : dayCount + 1);
            }

            while (refundCount <= maxRemainder) {
                for (CardParam paramCard : filterParamCards) {
                    Integer dayCount = dayCountMap.get(paramCard.getCardNum());
                    //整除
                    Boolean pre = paramCard.getRemainder() == 0 && refundCount > paramCard.getAvePreCount().intValue();

                    Boolean day = paramCard.getRemainder() != 0 && dayCount > paramCard.getRemainder();
                    //非整除
                    Boolean remainder = paramCard.getRemainder() > 0 && refundCount > (day ? paramCard.getAvePreCount().intValue() : paramCard.getAvePreCount().intValue() + 1);

                    if (pre || remainder) {
                        continue;
                    }
                    PlanDetail planDetail = new PlanDetail();
                    planDetail.setCardNum(paramCard.getCardNum());
                    planDetail.setPlanNum(count + "");
                    planDetail.setOutputCadNum(snapshotPlanDetail.getCardNum());
                    if (totalCount != 0) {
                        initRollMoney = initRollMoney.multiply(ACTUAL_REFUND_RATE).setScale(2, BigDecimal.ROUND_DOWN);
                    }
//                    BigDecimal deposit2 = totalFee.multiply(ACTUAL_REFUND_RATE.pow(totalCount)).subtract(reduceServiceFee).setScale(2, BigDecimal.ROUND_DOWN);
                    BigDecimal rollMoney = snapshotPlanDetail.getRollMoney().subtract(snapshotPlanDetail.getReserveFee());
                    BigDecimal preDeposit = null == numDepositMap.get(paramCard.getCardNum()) ? BigDecimal.ZERO : numDepositMap.get(paramCard.getCardNum());
                    BigDecimal thisDeposit = preDeposit.add(rollMoney.multiply(ACTUAL_REFUND_RATE).setScale(2, BigDecimal.ROUND_UP));

                    BigDecimal waitReserve = null == waitReserveMap.get(paramCard.getCardNum()) ? paramCard.getRefundAmount() : waitReserveMap.get(paramCard.getCardNum());
                    if (waitReserve.compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }
                    PlanDetail planDetailExtra = null;
                    if (initRollMoney.compareTo(waitReserve) > 0) {
                        BigDecimal realDeposit = waitReserve.divide(ACTUAL_REFUND_RATE, 2, BigDecimal.ROUND_DOWN);
                        planDetail.setDeposit(waitReserve);
                        planDetail.setWaitReserveMoney(BigDecimal.ZERO);
                        BigDecimal reserveFee = realDeposit.multiply(SERVICE_FEE_RATE).setScale(2, BigDecimal.ROUND_DOWN);
                        planDetail.setReserveFee(reserveFee);
                        planDetail.setRollMoney(rollMoney);
                        planDetail.setRealDeposit(realDeposit);
                        numDepositMap.put(paramCard.getCardNum(), waitReserve);
                        waitReserveMap.put(paramCard.getCardNum(), BigDecimal.ZERO);

                        if (!planDetail.getCardNum().equals(snapshotPlanDetail.getCardNum())) {
                            //回款
                            planDetailExtra = new PlanDetail();
                            BigDecimal depositExtra = waitReserve.multiply(ACTUAL_REFUND_RATE).setScale(2, BigDecimal.ROUND_DOWN);
                            planDetailExtra.setDeposit(depositExtra);
                            planDetailExtra.setCardNum(snapshotPlanDetail.getCardNum());
                            planDetailExtra.setPlanNum(count + "");
                            planDetailExtra.setOutputCadNum(planDetail.getCardNum());
                            planDetailExtra.setReserveFee(waitReserve.multiply(SERVICE_FEE_RATE).setScale(2, BigDecimal.ROUND_UP));
                            planDetailExtra.setWaitReserveMoney(snapshotPlanDetail.getWaitReserveMoney());
                            planDetailExtra.setRollMoney(planDetail.getRollMoney().subtract(planDetail.getReserveFee()));
                            planDetailExtra.setRealDeposit(waitReserve);
                            planDetails.add(planDetailExtra);

                            snapshotPlanDetail = planDetailExtra;
                        }
                    } else {
                        planDetail.setDeposit(rollMoney.multiply(ACTUAL_REFUND_RATE).setScale(2, BigDecimal.ROUND_UP));
                        planDetail.setWaitReserveMoney(paramCard.getRefundAmount().subtract(thisDeposit));
                        planDetail.setReserveFee(rollMoney.multiply(SERVICE_FEE_RATE).setScale(2, BigDecimal.ROUND_DOWN));
                        planDetail.setRollMoney(snapshotPlanDetail.getRollMoney().subtract(snapshotPlanDetail.getReserveFee()));
                        planDetail.setRealDeposit(rollMoney);

                        numDepositMap.put(paramCard.getCardNum(), thisDeposit);
                        waitReserveMap.put(paramCard.getCardNum(), planDetail.getWaitReserveMoney());
                        snapshotPlanDetail = planDetail;
                    }
                    planDetail.setDate(vernier);
                    totalCount++;

                    logger.info("planDetailA: {}", planDetail);
                    if (null != planDetailExtra) {
//                        logger.info("reduceServiceFee:{}", reduceServiceFee);
                        logger.info("planDetailB: {}", planDetailExtra);
                    }
                    planDetails.add(planDetail);
//                    Integer depositCount = depositCountMap.get(paramCard.getCardNum());
//                    depositCountMap.put(paramCard.getCardNum(), null == depositCount ? 1 : depositCount + 1);
                }
                refundCount++;
            }

            vernier = DateUtils.addDays(vernier, 1);
            count++;
        }


        logger.info("********** result **********\n");
        BigDecimal totalServiceFee = new BigDecimal(0);
        for (PlanDetail planDetail : planDetails) {
            totalServiceFee = totalServiceFee.add(planDetail.getReserveFee());
        }
        logger.info("totalServiceFee:{}", totalServiceFee);
    }

    private List<CardParam> filterByDate(List<CardParam> filterParamCards, Date vernier, Map<String, Integer> dayCountMap) {
        List<CardParam> list = new ArrayList<>();
        for (CardParam paramCard : filterParamCards) {
            if (vernier.getTime() >= paramCard.getPayStartDate().getTime() && vernier.getTime() <= paramCard.getPayEndDate().getTime()) {
                list.add(paramCard);
            }
        }
        return list;
    }

}
