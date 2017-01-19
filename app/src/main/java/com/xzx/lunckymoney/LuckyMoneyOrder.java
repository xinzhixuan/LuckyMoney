package com.xzx.lunckymoney;

/**
 * Created by xinzhixuan on 2017/1/19.
 * 抢红包流程；使用1，2，4，。。。。考虑以后使用位运算
 */
public enum LuckyMoneyOrder {

    /**
     * 任务初识状态
     */
    INIT(-1),
    /**
     * 收到红包消息
     */
    RECEIVED_MONEY(1),
    /**
     * 点击红包
     */
    CLICK_MONEY(2),
    /**
     * 点击“开”按钮
     */
    CLICK_KAI_BTN_FINSHED(4),
    /**
     * 红包详情页面
     */
    MONEY_DESC(8),
    /**
     * 流程完成
     */
    FINISHED(Integer.MAX_VALUE);
    private final int value;
    LuckyMoneyOrder(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
