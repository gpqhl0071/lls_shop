package org.example.model;

import java.math.BigDecimal;

public class ProductSummary {
    private String flagId;
    private BigDecimal amount;
    private String publishTime;
    private String introduction;
    private int favoriteCount;
    private int serverId;
    private long combatPower;
    private int vipLevel;
    private long killScore;
    private String name; // 新增字段

    // 构造函数
    public ProductSummary() {}

    public ProductSummary(String flagId, BigDecimal amount, String publishTime, String introduction, int favoriteCount,
                          int serverId, long combatPower, int vipLevel, long killScore, String name) {
        this.flagId = flagId;
        this.amount = amount;
        this.publishTime = publishTime;
        this.introduction = introduction;
        this.favoriteCount = favoriteCount;
        this.serverId = serverId;
        this.combatPower = combatPower;
        this.vipLevel = vipLevel;
        this.killScore = killScore;
        this.name = name;
    }

    // Getters and Setters
    public String getFlagId() {
        return flagId;
    }

    public void setFlagId(String flagId) {
        this.flagId = flagId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(String publishTime) {
        this.publishTime = publishTime;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(int favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public long getCombatPower() {
        return combatPower;
    }

    public void setCombatPower(long combatPower) {
        this.combatPower = combatPower;
    }

    public int getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(int vipLevel) {
        this.vipLevel = vipLevel;
    }

    public long getKillScore() {
        return killScore;
    }

    public void setKillScore(long killScore) {
        this.killScore = killScore;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ProductSummary{" +
                "flagId='" + flagId + '\'' +
                ", amount=" + amount +
                ", publishTime='" + publishTime + '\'' +
                ", introduction='" + introduction + '\'' +
                ", favoriteCount=" + favoriteCount +
                ", serverId=" + serverId +
                ", combatPower=" + combatPower +
                ", vipLevel=" + vipLevel +
                ", killScore=" + killScore +
                ", name='" + name + '\'' +
                '}';
    }
}