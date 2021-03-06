package com.jeekhan.wx.model;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MediaMaterial {
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	
    private BigInteger id;

    private String mediaType;

    private String isTemp;

    private String mediaId;

    private String mediaUrl;

    private String isNewsImg;

    private Date updateTime;

    private String status;

    private Integer updateUser;

    private String content;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType == null ? null : mediaType.trim();
    }

    public String getIsTemp() {
        return isTemp;
    }

    public void setIsTemp(String isTemp) {
        this.isTemp = isTemp == null ? null : isTemp.trim();
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId == null ? null : mediaId.trim();
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl == null ? null : mediaUrl.trim();
    }

    public String getIsNewsImg() {
        return isNewsImg;
    }

    public void setIsNewsImg(String isNewsImg) {
        this.isNewsImg = isNewsImg == null ? null : isNewsImg.trim();
    }

    public String getUpdateTime() {
    		if(this.updateTime != null) {
    			return format.format(updateTime);
    		}else {
    			return null;
    		}
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public Integer getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(Integer updateUser) {
        this.updateUser = updateUser;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
    }
}